package com.klein.btc.repository;

import com.klein.btc.model.Exchange;
import com.klein.btc.model.Product;
import com.klein.btc.model.Ticks;
import com.klein.btc.model.TicksId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Created by mresc on 05.11.17.
 */
public interface TicksRepository extends JpaRepository<Ticks, TicksId>{
    Page<Ticks> findById_exchangeAndId_productAndId_timestampBetweenOrderById_timestampDesc(Exchange exchange, Product product, long tsFrom, long tsTill, Pageable pageable);
}
