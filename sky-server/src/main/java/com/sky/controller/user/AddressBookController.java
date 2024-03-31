package com.sky.controller.user;

import com.sky.context.BaseContext;
import com.sky.entity.AddressBook;
import com.sky.result.Result;
import com.sky.service.AddressBookService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "C端-地址簿接口")
@RestController
@RequestMapping("/user/addressBook")
@Slf4j
public class AddressBookController {
    @Autowired
    private AddressBookService addressBookService;

    /**
     * 新增地址
     *
     * @param addressBook
     * @return
     */
    @ApiOperation("新增地址")
    @PostMapping
    public Result add(@RequestBody AddressBook addressBook) {
        log.info("新增地址：{}", addressBook);
        addressBookService.add(addressBook);
        return Result.success();
    }

    /**
     * 查询当前登录用户的所有地址信息
     *
     * @return
     */
    @ApiOperation("查询当前登录用户的所有地址信息")
    @GetMapping("/list")
    public Result<List<AddressBook>> list() {
        AddressBook addressBook = new AddressBook();
        addressBook.setUserId(BaseContext.getCurrentId());
        List<AddressBook> addressBookList = addressBookService.list(addressBook);
        return Result.success(addressBookList);
    }

    /**
     * 查询默认地址
     *
     * @return
     */
    @ApiOperation("查询默认地址")
    @GetMapping("/default")
    public Result<AddressBook> queryDefaultAddress() {
        Long userId = BaseContext.getCurrentId();
        AddressBook addressBook = AddressBook.builder().userId(userId).isDefault(1).build();
        List<AddressBook> list = addressBookService.list(addressBook);

        if (list.size() > 0 && list != null) {
            return Result.success(list.get(0));
        }

        return Result.error("没有查询到默认地址");
    }

    /**
     * 设置默认地址
     *
     * @return
     */
    @ApiOperation("设置默认地址")
    @PutMapping("/default")
    public Result updateDefaultAddress(@RequestBody AddressBook addressBook) {
        addressBookService.updateDefaultAddress(addressBook);
        return Result.success();
    }

    /**
     * 根据Id查询地址
     *
     * @return
     */
    @ApiOperation("根据Id查询地址")
    @GetMapping("/{id}")
    public Result<AddressBook> queryById(@PathVariable Long id) {
        AddressBook addressBook = addressBookService.queryById(id);
        return Result.success(addressBook);
    }

    /**
     * 根据Id删除地址
     *
     * @return
     */
    @ApiOperation("根据Id删除地址")
    @DeleteMapping
    public Result deleteById(Long id) {
        addressBookService.deleteById(id);
        return Result.success();
    }

    /**
     * 根据id修改地址
     *
     * @return
     */
    @ApiOperation("根据id修改地址")
    @PutMapping
    public Result updateById(@RequestBody AddressBook addressBook) {
        addressBookService.updateById(addressBook);
        return Result.success();
    }
}