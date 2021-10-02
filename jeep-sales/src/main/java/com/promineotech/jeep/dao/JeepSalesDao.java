package com.promineotech.jeep.dao;

import com.promineotech.jeep.entity.Jeep;
import com.promineotech.jeep.entity.JeepModel;

import java.util.List;

public interface JeepSalesDao {

    List<Jeep> fetchJeeps(JeepModel model, String trim);
}
