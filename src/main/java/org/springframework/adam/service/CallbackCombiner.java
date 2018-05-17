/**
 * 
 */
package org.springframework.adam.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.adam.common.bean.ResultVo;
import org.springframework.adam.service.chain.ServiceChain;

/**
 * @author USER
 *
 */
public class CallbackCombiner<IncomeType, OutputType> extends AbsCallbacker<Object, Throwable, IncomeType, OutputType> {

	private List<AbsCallbacker> callbacks = new ArrayList<AbsCallbacker>();

	public CallbackCombiner() {
		super(Thread.currentThread().getId());
	}

	public CallbackCombiner(ThreadPoolExecutor tpe) {
		super(Thread.currentThread().getId());
		this.tpe = tpe;
	}

	public void combine(AbsCallbacker<Object, Throwable, IncomeType, OutputType> callback) {
		if (null == callback) {
			return;
		}
		callback.setCombiner(this);
		this.callbacks.add(callback);
	}

	/**
	 * 塞进service chain income output
	 * 
	 * @param serviceChain
	 * @param income
	 * @param output
	 */
	@Override
	public void setChain(ServiceChain serviceChain, IncomeType income, ResultVo<OutputType> output) {
		this.serviceChain = serviceChain;
		this.income = income;
		this.output = output;
		for (AbsCallbacker absCallbacker : this.callbacks) {
			// 如果有一个callback没做完就不继续做下去
			absCallbacker.setChain(serviceChain, income, output);
		}
		latch.countDown();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.adam.service.AbsCallbacker#onComplete(java.lang.
	 * Object, java.lang.Throwable)
	 * 
	 * 重写onColmplete方法
	 */
	@Override
	public void onComplete(Object result, Throwable e) {
		// 允许多次调用onComplete
		for (AbsCallbacker absCallbacker : this.callbacks) {
			// 如果有一个callback没做完就不继续做下去
			if (!absCallbacker.isDone()) {
				return;
			}
		}
		onDoIt(null, e, COMPL_METHOD);
	}

	@Override
	public void dealSuccess(Object result) {
		// nothing to do
	}

	@Override
	public void dealFail(Throwable e) {
		// nothing to do
	}

	@Override
	public void dealComplete(Object result, Throwable e) {
		// nothing to do
	}

	@Override
	public void dealException(Throwable t) {
		// nothing to do
	}

}