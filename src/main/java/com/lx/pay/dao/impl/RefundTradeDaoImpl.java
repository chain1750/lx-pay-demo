package com.lx.pay.dao.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lx.pay.dao.RefundTradeDAO;
import com.lx.pay.dao.entity.RefundTrade;
import com.lx.pay.dao.mapper.RefundTradeMapper;
import org.springframework.stereotype.Service;

/**
 * @author chenhaizhuang
 */
@Service
public class RefundTradeDaoImpl extends ServiceImpl<RefundTradeMapper, RefundTrade> implements RefundTradeDAO {
}
