package com.lxp.aplus.payment.application.port.in;

import com.lxp.aplus.payment.application.port.in.model.command.PaymentConfirmCommand;
import com.lxp.aplus.payment.application.port.in.model.command.PaymentPrepareCommand;
import com.lxp.aplus.payment.application.port.in.model.result.PaymentPrepareResult;

public interface PaymentUseCase {

    PaymentPrepareResult prepare(PaymentPrepareCommand command);

    void confirm(PaymentConfirmCommand command);
}
