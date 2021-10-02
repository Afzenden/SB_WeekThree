package com.promineotech.jeep.controller;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;
import com.promineotech.jeep.entity.Jeep;
import com.promineotech.jeep.entity.JeepModel;
import com.promineotech.jeep.service.JeepSalesService;

import lombok.extern.slf4j.Slf4j;


@RestController
@Slf4j
public class DefaultJeepSalesController implements JeepSalesController {

  @Autowired
  private JeepSalesService jeepSalesService;
	
  @Override		
  public List<Jeep> fetchJeeps(JeepModel model, String trim) {
    log.debug("model={}", "trim={}", model, trim);
    return jeepSalesService.fetchJeeps(model, trim);
  }

  protected List<Jeep> buildExpected() {
    List<Jeep> list = new LinkedList<>();

    // @formatter:off
    list.add(Jeep.builder()
            .modelId(JeepModel.WRANGLER)
            .trimLevel("Sport")
            .numDoors(4)
            .wheelSize(17)
            .basePrice(new BigDecimal("31975.00"))
            .build());
    list.add(Jeep.builder()
            .modelId(JeepModel.WRANGLER)
            .trimLevel("Sport")
            .numDoors(2)
            .wheelSize(17)
            .basePrice(new BigDecimal("28475.00"))
            .build());


    // @formatter:on

    Collections.sort(list);
    return list;
	}

  protected void assertErrorMessageValid(Map<String, Object> error, HttpStatus status) {
    assertThat(error)
            .containsKey("message")
            .containsEntry("status code", status.value())
            .containsEntry("uri", "/jeeps")
            .containsKey("timestamp")
            .containsEntry("reason", status.getReasonPhrase());
  }


}
