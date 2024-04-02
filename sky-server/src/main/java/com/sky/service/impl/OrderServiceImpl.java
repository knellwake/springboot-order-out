package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.*;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.print.DocFlavor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    String number;

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

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
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
        Long currentId = BaseContext.getCurrentId();

        OrderStatisticsVO orderStatisticsVO = orderMapper.countStatus();

        return orderStatisticsVO;
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