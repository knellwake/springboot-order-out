package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.HttpClientUtil;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.*;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private UserMapper userMapper;

    private String number;

    @Autowired
    private WebSocketServer webSocketServer;

    @Value("${sky.shop.address}")
    private String shopAddress;

    @Value("${sky.baidu.AK}")
    private String ak;

    /**
     * 检查客户的收货地址是否超出配送范围
     *
     * @param address
     */
    private void checkOutOfRange(String address) {
        Map map = new HashMap();
        map.put("address", shopAddress);
        map.put("output", "json");
        map.put("ak", ak);

        //获取店铺的经纬度坐标
        String shopCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);

        JSONObject jsonObject = JSON.parseObject(shopCoordinate);
        if (!jsonObject.getString("status").equals("0")) {
            throw new OrderBusinessException("店铺地址解析失败");
        }

        //数据解析
        JSONObject location = jsonObject.getJSONObject("result").getJSONObject("location");
        String lat = location.getString("lat");
        String lng = location.getString("lng");
        //店铺经纬度坐标
        String shopLngLat = lat + "," + lng;

        map.put("address", address);
        //获取用户收货地址的经纬度坐标
        String userCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);

        jsonObject = JSON.parseObject(userCoordinate);
        if (!jsonObject.getString("status").equals("0")) {
            throw new OrderBusinessException("收货地址解析失败");
        }

        //数据解析
        location = jsonObject.getJSONObject("result").getJSONObject("location");
        lat = location.getString("lat");
        lng = location.getString("lng");
        //用户收货地址经纬度坐标
        String userLngLat = lat + "," + lng;

        map.put("origin", shopLngLat);
        map.put("destination", userLngLat);
        map.put("steps_info", "0");

        //路线规划
        String json = HttpClientUtil.doGet("https://api.map.baidu.com/directionlite/v1/driving", map);

        jsonObject = JSON.parseObject(json);
        if (!jsonObject.getString("status").equals("0")) {
            throw new OrderBusinessException("配送路线规划失败");
        }

        //数据解析
        JSONObject result = jsonObject.getJSONObject("result");
        JSONArray jsonArray = (JSONArray) result.get("routes");
        Integer distance = (Integer) ((JSONObject) jsonArray.get(0)).get("distance");

        if (distance > 5000) {
            //配送距离超过5000米
            throw new OrderBusinessException("超出配送范围");
        }
    }

    /**
     * 用户下单
     *
     * @param ordersSubmitDTO
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        // 处理各种业务异常（地址簿为空，购物车为空）
        // 判断地址簿为空 -> 为空直接打回
        Long addressBookId = ordersSubmitDTO.getAddressBookId();
        AddressBook addressBook = addressBookMapper.selectById(addressBookId);
        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        // 检查用户地址范围，是否可以送达：
        checkOutOfRange(addressBook.getCityName() + addressBook.getDistrictName() + addressBook.getDetail());

        // 判断购物车是否为空
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = ShoppingCart.builder().userId(userId).build();
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);
        if (shoppingCartList.size() == 0 || shoppingCartList == null) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        // 插入一条订单表数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setUserId(userId);
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());

        orderMapper.insert(orders);

        // 插入N条订单明细表
        List<OrderDetail> list = new ArrayList<>();
        for (ShoppingCart cart : shoppingCartList) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            //与订单明细表关联的订单Id
            orderDetail.setOrderId(orders.getId());
            list.add(orderDetail);
        }
        orderDetailMapper.insertBatch(list);

        // 清空购物车内的数据
        shoppingCartMapper.delectAllByUserId(userId);
        // 封装VO返回结果
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderTime(LocalDateTime.now())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .build();


        number = orders.getNumber();

        return orderSubmitVO;
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
        //JSONObject jsonObject = weChatPayUtil.pay(
        //        ordersPaymentDTO.getOrderNumber(), //商户订单号
        //        new BigDecimal(0.01), //支付金额，单位 元
        //        "道格外卖订单", //商品描述
        //        user.getOpenid() //微信用户的openid
        //);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code", "ORDERPAID");

        //if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
        //    throw new OrderBusinessException("该订单已支付");
        //}

        // 使用模拟支付,前端已经设置重定向到支付成功页面
        // 所以后端 直接更新 数据库数据就可以了，这里直接拿下面的支付成功修改订单状态的方法，因为已经写好了要修改的数据
        // number订单号 直接从上面的用户下单方法拿来用
        paySuccess(number);

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();

        // 根据订单号查询当前用户的订单
        Orders ordersDB = orderMapper.getByNumberAndUserId(outTradeNo, userId);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);

        // 通过websocket向客户端浏览器发送推送信息 type orderId content
        Map map = new HashMap();
        map.put("type", 1);//消息类型，1表示来单提醒,2表示客户催单
        map.put("orderId", orders.getId());
        map.put("content", "订单号：" + outTradeNo);

        //通过WebSocket实现来单提醒，向客户端浏览器推送消息
        String jsonString = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(jsonString);
    }

    /**
     * 查询历史订单
     *
     * @param ordersPageQueryDTO
     * @return
     */
    @Override
    public PageResult queryHistoryOrders(OrdersPageQueryDTO ordersPageQueryDTO) {
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Page<OrderVO> ordersPage = orderMapper.selectOrders(ordersPageQueryDTO);
        List<OrderVO> result = ordersPage.getResult();
        for (OrderVO orderVO : result) {
            List<OrderDetail> orderDetailList = orderDetailMapper.selectBatchByOrderId(orderVO.getId());
            orderVO.setOrderDetailList(orderDetailList);
        }

        return new PageResult(ordersPage.getTotal(), ordersPage.getResult());
    }

    /**
     * 查询订单详情
     *
     * @param id
     * @return
     */
    @Override
    public OrderVO showDetails(Long id) {
        // 查询当前订单的多个订单详情 一对多
        // 当前用户的 对应订单id
        List<OrderDetail> orderDetails = orderDetailMapper.selectBatchByOrderId(id);
        OrderVO orderVO = orderMapper.selectOrdersById(id);
        orderVO.setOrderDetailList(orderDetails);
        orderVO.setAddress(addressBookMapper.selectById(orderVO.getAddressBookId()).getDetail());
        return orderVO;
    }

    /**
     * 取消订单
     *
     * @param id
     */
    @Override
    public void cancelOrder(Long id) {
        OrderVO orderVO = orderMapper.selectOrdersById(id);
        // 修改订单状态:
        orderVO.setStatus(Orders.CANCELLED);
        if (orderVO.getPayStatus().equals(Orders.PAID)) {
            //退款逻辑
            orderVO.setPayStatus(Orders.REFUND);
        }
        Orders orders = new Orders();
        BeanUtils.copyProperties(orderVO, orders);
        orderMapper.update(orders);
    }

    /**
     * 再来一单
     *
     * @param id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void repetitionOrder(Long id) {
        // 根据原订单Id 查询原订单表与订单详情
        // 获取原订单的订单详情 添加进购物车
        List<OrderDetail> orderDetails = orderDetailMapper.selectBatchByOrderId(id);

        for (OrderDetail orderDetail : orderDetails) {
            ShoppingCart shoppingCart = new ShoppingCart();
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCart.setUserId(BaseContext.getCurrentId());
            BeanUtils.copyProperties(orderDetail, shoppingCart);
            shoppingCartMapper.insert(shoppingCart);
        }
    }

    /**
     * 分页查询显示
     *
     * @param ordersPageQueryDTO
     * @return
     */
    @Override
    public PageResult queryPage(OrdersPageQueryDTO ordersPageQueryDTO) {
        // 分页
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Page<OrderVO> ordersPage = orderMapper.selectOrdersByCondition(ordersPageQueryDTO);
        // 菜品信息设置
        List<OrderVO> ordersList = getOrderVOList(ordersPage);
        return new PageResult(ordersPage.getTotal(), ordersList);
    }

    /**
     * 查询订单详情
     *
     * @param id
     * @return
     */
    @Override
    public OrderVO queryOrderDetails(Long id) {
        OrderVO orderVO = orderMapper.selectOrdersById(id);

        List<OrderDetail> orderDetails = orderDetailMapper.selectBatchByOrderId(orderVO.getId());
        orderVO.setOrderDetailList(orderDetails);

        return orderVO;
    }

    /**
     * 各个状态的订单数量统计
     *
     * @return
     */
    @Override
    public OrderStatisticsVO countOrderStatus() {
        OrderStatisticsVO orderStatisticsVO = orderMapper.countStatus();
        return orderStatisticsVO;
    }

    /**
     * 接单
     *
     * @param ordersConfirmDTO
     */
    @Override
    public void updateConfirm(OrdersConfirmDTO ordersConfirmDTO) {
        OrderVO orderVO = orderMapper.selectOrdersById(ordersConfirmDTO.getId());
        Orders orders = new Orders();
        BeanUtils.copyProperties(orderVO, orders);
        if (orders.getPayStatus().equals(Orders.PAID)) {
            orders.setStatus(Orders.CONFIRMED);
        }
        orderMapper.update(orders);
    }

    /**
     * 拒单
     *
     * @param ordersRejectionDTO
     */
    @Override
    public void updateRejection(OrdersRejectionDTO ordersRejectionDTO) {
        OrderVO orderVO = orderMapper.selectOrdersById(ordersRejectionDTO.getId());
        Orders orders = new Orders();
        BeanUtils.copyProperties(orderVO, orders);
        orders.setStatus(Orders.CANCELLED);
        orders.setRejectionReason(orders.getRejectionReason());
        orders.setCancelTime(LocalDateTime.now());
        orders.setPayStatus(Orders.REFUND);

        orderMapper.update(orders);
    }

    /**
     * 取消订单
     *
     * @param ordersCancelDTO
     */
    @Override
    public void updateCancel(OrdersCancelDTO ordersCancelDTO) {
        OrderVO orderVO = orderMapper.selectOrdersById(ordersCancelDTO.getId());
        Orders orders = new Orders();
        BeanUtils.copyProperties(orderVO, orders);
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelTime(LocalDateTime.now());
        orders.setCancelReason(ordersCancelDTO.getCancelReason());
        orders.setPayStatus(Orders.REFUND);

        orderMapper.update(orders);
    }

    /**
     * 派送订单
     *
     * @param id
     */
    @Override
    public void updateDelivery(Long id) {
        OrderVO orderVO = orderMapper.selectOrdersById(id);
        Orders orders = new Orders();
        BeanUtils.copyProperties(orderVO, orders);
        orders.setId(id);
        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);
        orderMapper.update(orders);
    }

    /**
     * 完成订单
     *
     * @param id
     */
    @Override
    public void updateComplete(Long id) {
        OrderVO orderVO = orderMapper.selectOrdersById(id);
        Orders orders = new Orders();
        BeanUtils.copyProperties(orderVO, orders);
        orders.setId(id);
        orders.setStatus(Orders.COMPLETED);
        orders.setDeliveryTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    /**
     * 催单提醒
     *
     * @param id
     */
    @Override
    public void reminder(Long id) {
        OrderVO orderVO = orderMapper.selectOrdersById(id);
        if (orderVO == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        Map map = new HashMap();
        map.put("type", 2);//消息类型，1表示来单提醒,2表示客户催单
        map.put("orderId", id);
        map.put("content", "订单号：" + orderVO.getNumber());

        webSocketServer.sendToAllClient(JSON.toJSONString(map));
    }

    /**
     * 根据订单id获取菜品信息字符串,设置订单数据显示与格式配置
     *
     * @return
     */
    public List<OrderVO> getOrderVOList(Page<OrderVO> ordersPage) {
        List<OrderVO> ordersList = ordersPage.getResult();
        // 获取每一条订单
        for (OrderVO orderVO : ordersList) {
            //订单地址设置
            orderVO.setAddress(addressBookMapper.selectById(orderVO.getAddressBookId()).getDetail());

            //订单详情表获取
            List<OrderDetail> orderDetails = orderDetailMapper.selectBatchByOrderId(orderVO.getId());
            // 订单详情表 菜品与数量 获取
            String orderDishStr = getOrderDishStr(orderDetails);
            orderVO.setOrderDishes(orderDishStr);

            for (OrderDetail orderDetail : orderDetails) {
                // 订单菜品口味备注设置
                log.info("订单菜品口味:{}", orderDetail.getDishFlavor());
                if (orderDetail.getDishFlavor() != null) {
                    orderVO.setRemark(orderDetail.getDishFlavor() + ";" + orderVO.getRemark());
                } else {
                    orderVO.setRemark(orderVO.getRemark());
                }
            }
        }
        return ordersList;
    }

    /**
     * 根据订单id获取菜品信息字符串
     *
     * @return
     */
    public String getOrderDishStr(List<OrderDetail> orderDetails) {
        List<String> stringList = orderDetails.stream().map(x -> {
            String orderDishNameNumber = x.getName() + "*" + x.getNumber() + ";";
            return orderDishNameNumber;
        }).collect(Collectors.toList());

        String join = String.join("", stringList);
        return join;
    }
}