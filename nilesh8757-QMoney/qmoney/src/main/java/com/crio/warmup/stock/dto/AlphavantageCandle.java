package com.crio.warmup.stock.dto;

import java.sql.Date;
import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// Reference - https:www.baeldung.com/jackson-ignore-properties-on-serialization
// Reference - https:www.baeldung.com/jackson-name-of-property
@JsonIgnoreProperties(ignoreUnknown = true)
public class AlphavantageCandle implements Candle {
  @JsonProperty("1. open")
  private Double open;
  @JsonProperty("4. close")
  private Double close;
  @JsonProperty("2. high")
  private Double high;
  @JsonProperty("3. low")
  private Double low;
  private LocalDate date;

  @Override
  public Double getOpen() {
    // TODO Auto-generated method stub
    return this.open;
  }

  @Override
  public Double getClose() {
    // TODO Auto-generated method stub
    return this.close;
  }

  @Override
  public Double getHigh() {
    // TODO Auto-generated method stub
    return this.high;
  }

  @Override
  public Double getLow() {
    // TODO Auto-generated method stub
    return this.low;
  }

  @Override
  public LocalDate getDate() {
    // TODO Auto-generated method stub
    return date;
  }

  public void setDate(LocalDate keyDate) {
    this.date = keyDate;
  }

  @Override
  public String toString() {
    return "AlphavantageCandle{" + "open=" + open + ", close=" + close + ", high=" + high + ", low=" + low + ", date="
        + date + '}';
  }
}
