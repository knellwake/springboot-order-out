package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.entity.AddressBook;
import com.sky.mapper.AddressBookMapper;
import com.sky.service.AddressBookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Service
public class AddressBookServiceImpl implements AddressBookService {

    @Autowired
    private AddressBookMapper addressBookMapper;

    /**
     * 新增地址
     *
     * @param addressBook
     */
    @Override
    public void add(@RequestBody AddressBook addressBook) {
        Long currentId = BaseContext.getCurrentId();
        addressBook.setUserId(currentId);
        //设置新增都不是默认地址
        addressBook.setIsDefault(0);
        addressBookMapper.insert(addressBook);
    }

    /**
     * 查询当前登录用户的所有地址信息
     *
     * @return
     */
    @Override
    public List<AddressBook> list(AddressBook addressBook) {
        List<AddressBook> list = addressBookMapper.list(addressBook);
        return list;
    }

    /**
     * 根据Id查询地址
     *
     * @param id
     * @return
     */
    @Override
    public AddressBook queryById(Long id) {
        AddressBook addressBook = addressBookMapper.selectById(id);
        return addressBook;
    }

    /**
     * 根据id删除地址
     *
     * @param id
     */
    @Override
    public void deleteById(Long id) {
        addressBookMapper.deleteById(id);
    }

    /**
     * 根据id修改地址
     *
     * @param addressBook
     */
    @Override
    public void updateById(AddressBook addressBook) {
        addressBookMapper.updateById(addressBook);
    }

    /**
     * 设置默认地址
     *
     * @param addressBook 传递过来的是地址Id
     */
    @Override
    public void updateDefaultAddress(AddressBook addressBook) {
        // 先将全部地址默认值修改为0
        AddressBook addressBook1 = new AddressBook();
        addressBook1.setUserId(BaseContext.getCurrentId());
        addressBook1.setIsDefault(0);
        addressBookMapper.updateByUserId(addressBook1);
        // 再根据传递的地址Id改为默认1
        addressBook.setIsDefault(1);
        addressBookMapper.updateById(addressBook);
    }
}