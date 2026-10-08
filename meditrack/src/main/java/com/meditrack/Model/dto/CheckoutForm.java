package com.meditrack.dto;

import com.meditrack.enums.OrderChannel;
import com.meditrack.enums.PaymentMethod;
import java.math.BigDecimal;

public class CheckoutForm {

    private Long orderId;
    private OrderChannel channel = OrderChannel.ONLINE;
    private Long customerId;
    private PaymentMethod paymentMethod = PaymentMethod.CARD;
    private BigDecimal cashTendered;
    private String cardNumber;
    private String cardHolder;
    private String expiry;
    private String cvv;
    private BigDecimal acceptedQuoteTotal;
    private String attemptKey;

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public OrderChannel getChannel() { return channel; }
    public void setChannel(OrderChannel channel) { this.channel = channel; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    public BigDecimal getCashTendered() { return cashTendered; }
    public void setCashTendered(BigDecimal cashTendered) { this.cashTendered = cashTendered; }
    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
    public String getCardHolder() { return cardHolder; }
    public void setCardHolder(String cardHolder) { this.cardHolder = cardHolder; }
    public String getExpiry() { return expiry; }
    public void setExpiry(String expiry) { this.expiry = expiry; }
    public String getCvv() { return cvv; }
    public void setCvv(String cvv) { this.cvv = cvv; }
    public BigDecimal getAcceptedQuoteTotal() { return acceptedQuoteTotal; }
    public void setAcceptedQuoteTotal(BigDecimal acceptedQuoteTotal) { this.acceptedQuoteTotal = acceptedQuoteTotal; }
    public String getAttemptKey() { return attemptKey; }
    public void setAttemptKey(String attemptKey) { this.attemptKey = attemptKey; }
}
