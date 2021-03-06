/**
 * 
 */
package com.rabbit.receiver.aspects;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.aop.ThrowsAdvice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rabbit.receiver.Application;
import com.rabbit.receiver.enums.QueueSuffixEnumeration;
import com.rabbit.receiver.services.impl.ReceiverServiceImpl;

/**
 * @author cbezmen
 *
 */
@Aspect
@Component
public class ErrorHandler implements ThrowsAdvice {

	@Autowired
	private RabbitTemplate template;

	private static final Logger LOG = LoggerFactory.getLogger(ReceiverServiceImpl.class);

	@AfterThrowing(pointcut = "execution(* com.rabbit.receiver.services.impl.ReceiverServiceImpl.receiveString(..))", throwing = "ex")
	public void can(final JoinPoint joinPoint, final Exception ex) {

		LOG.error(ex.getMessage());
		LOG.error("Sending to error queue");
		LOG.info("Sending args " + joinPoint.getArgs()[0]);
		MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
		RabbitListener rabbitListener = methodSignature.getMethod().getAnnotation(RabbitListener.class);
		for (String currentQueue : rabbitListener.queues()) {
			String errorQueue = createErrorQueueName(currentQueue);
			template.convertAndSend(errorQueue, joinPoint.getArgs()[0]);
			LOG.info("Moved to queue: " + currentQueue);
		}	
	
	}
	
	public String createErrorQueueName(String queueName){
		return queueName.concat(QueueSuffixEnumeration.ERROR_SUFFIX.getSuffix());
	}
}
