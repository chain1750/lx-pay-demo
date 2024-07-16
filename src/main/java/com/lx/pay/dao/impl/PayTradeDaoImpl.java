package com.lx.pay.dao.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lx.pay.dao.PayTradeDAO;
import com.lx.pay.dao.entity.PayTrade;
import com.lx.pay.dao.mapper.PayTradeMapper;
import org.springframework.stereotype.Service;

/**
 * @author chenhaizhuang
 */
@Service
public class PayTradeDaoImpl extends ServiceImpl<PayTradeMapper, PayTrade> implements PayTradeDAO {
}
