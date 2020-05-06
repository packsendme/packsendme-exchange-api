package com.packsendme.api.exchange.service;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.packsendme.api.exchange.component.CurrconvAPI_Component;
import com.packsendme.api.exchange.config.Cache_Config;
import com.packsendme.api.exchange.dao.ExchangeImpl_DAO;
import com.packsendme.exchange.bre.model.ExchangeBRE_Model;
import com.packsendme.lib.common.constants.generic.HttpExceptionPackSend;
import com.packsendme.lib.common.response.Response;
import com.packsendme.lib.utility.ConvertFormat;

@Service
@ComponentScan({"com.packsendme.api.exchange.component"})
public class Exchange_Services {
	
	@Autowired
	Cache_Config cacheConfig;
	
	@Autowired
	ExchangeImpl_DAO<ExchangeBRE_Model> exchangeBREImpl_DAO;
	
	@Autowired(required=true)
	CurrconvAPI_Component currconvAPI; 

	ExchangeBRE_Model exchangeModel = null;

	public ResponseEntity<?> getExchangeRate(String current) {
		Response<ExchangeBRE_Model> responseObj = null;
		ConvertFormat dateFormat = new ConvertFormat();
		
		try {
			String dtNowS = new Date().toString();
			Date dtNow = dateFormat.convertStringToDateShort(dtNowS);

			// (1) Find In Cache IF current exist
			ExchangeBRE_Model exchangeBRE = exchangeBREImpl_DAO.findOne(cacheConfig.exchangeBRE_SA, current);

			if(exchangeBRE != null) {
				
				System.out.println("-- -- ");
				System.out.println("-- DATA STRING  -- "+ dtNowS);
				System.out.println("-- DATA   -- "+ dtNow);
				System.out.println("-- -- ");
	
				if(exchangeBRE.dt_exchange.equals(dtNow)){
					System.out.println("-- -- ");
					System.out.println("-- HTTP NO-API  -- "+ exchangeBRE.dt_exchange);
					System.out.println("-- -- ");

					responseObj = new Response<ExchangeBRE_Model>(HttpExceptionPackSend.FOUND_EXCHANGE.value(),HttpExceptionPackSend.FOUND_EXCHANGE.getAction(), exchangeBRE);
				}
				else {
					 // (2) Find In api.currconv.com
					
					System.out.println("-- -- ");
					System.out.println("-- HTTP API  -- "+ exchangeBRE.dt_exchange);
					System.out.println("-- -- ");

					
					exchangeModel = currconvAPI.getExchangeCurrent(current, dtNow);
					responseObj = new Response<ExchangeBRE_Model>(HttpExceptionPackSend.FOUND_EXCHANGE.value(),HttpExceptionPackSend.FOUND_EXCHANGE.getAction(), exchangeModel);

					Thread t1 = new Thread(new Runnable() {
						@Override
				        public void run() {
				        	try {
								Thread.sleep(1000);
								exchangeBREImpl_DAO.add(cacheConfig.exchangeBRE_SA, exchangeModel.to, exchangeModel);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
				        }
				    });
					t1.start();
				}
			}
			else {
				 // (2) Find In api.currconv.com
				exchangeModel = currconvAPI.getExchangeCurrent(current, dtNow);
				responseObj = new Response<ExchangeBRE_Model>(HttpExceptionPackSend.FOUND_EXCHANGE.value(),HttpExceptionPackSend.FOUND_EXCHANGE.getAction(), exchangeModel);
				Thread t2 = new Thread(new Runnable() {
					@Override
			        public void run() {
			        	try {
							Thread.sleep(1000);
							exchangeBREImpl_DAO.add(cacheConfig.exchangeBRE_SA, exchangeModel.to, exchangeModel);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
			        }
			    });
				t2.start();
			}
			return new ResponseEntity<>(responseObj, HttpStatus.ACCEPTED);
		} catch (Exception e) {
			responseObj = new Response<ExchangeBRE_Model>(HttpExceptionPackSend.FAIL_EXECUTION.value(),HttpExceptionPackSend.EXCHANGE_RATE.getAction(), null);
			return new ResponseEntity<>(responseObj, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	
}
