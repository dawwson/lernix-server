package com.lxp.aplus.payment.application.port.in;

import com.lxp.aplus.payment.application.command.PaymentConfirmCommand;
import com.lxp.aplus.payment.application.command.PaymentPrepareCommand;
import com.lxp.aplus.payment.application.result.PaymentPrepareResult;

public interface PaymentUseCase {

    PaymentPrepareResult prepare(PaymentPrepareCommand command);

    void confirm(PaymentConfirmCommand command);
}
