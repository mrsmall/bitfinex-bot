package com.klein.btc.repository;

import com.klein.btc.model.Ticks;
import com.klein.btc.model.TicksId;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Created by mresc on 05.11.17.
 */
public interface TicksRepository extends JpaRepository<Ticks, TicksId>{
}
