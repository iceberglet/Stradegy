package com.stradegy.history.analyser.strategies;

import com.stradegy.enums.BuySell;
import com.stradegy.history.analyser.MarketDataContainer;
import com.stradegy.history.analyser.actions.TradeAction;
import com.stradegy.history.analyser.indicators.MACDIndicator;
import com.stradegy.utils.Logger;

import java.util.Arrays;

/**
 * Created by User on 24/1/2017.
 */
public class MovingAverageStrategy extends Strategy {

	private Double prevSignal;
	private Double prevLag;
	private Double prevLongSignal;
	private Double prevLongLag;

	private Double notionalRatio = 0.2;

	public MovingAverageStrategy(int slow, int fast, int signal, int superSlow, int superFast, int superSignal){
		super(new Portfolio(10000D), Arrays.asList(new MACDIndicator(fast, slow, signal),
														  new MACDIndicator(superFast, superSlow, superSignal)));
	}

	@Override
	public void update(MarketDataContainer marketData) {
		MACDIndicator macd = (MACDIndicator)this.indicators.get(0);
		MACDIndicator macdLongTerm = (MACDIndicator)this.indicators.get(1);
		if(!macd.isReady() || !macdLongTerm.isReady())
			return;
		Logger.emit(this.getClass().getSimpleName(), marketData.getLast().getDay() + " - Received New Market Data, Current Portfolio: " + this.portfolio.netWorth(marketData));
		Double signal = macd.getSignalEMA();
		Double lag = macd.getValue();
		Double longSignal = macdLongTerm.getSignalEMA();
		Double longLag = macdLongTerm.getValue();
		Double currentPrice = marketData.getLast().getCandle().getClose();

//		Logger.emit(this.getClass().getSimpleName(), signal + " " + lag + " " + longLag + " " + longSignal);

		if(prevLag == null || prevSignal == null || prevLongLag == null || prevLongSignal == null){
			prevLag = lag;
			prevSignal = signal;
			prevLongLag = longLag;
			prevLongSignal = longSignal;
		} else {
			//Compare the previous with current, and decide on position to take

			//If currently holding short position
			if(portfolio.getPosition().getNotional() < 0){
				if(signal > lag){
					TradeAction tradeAction = new TradeAction(null, Math.abs(portfolio.getPosition().getNotional()),
							marketData.getLast().getCandle().getClose(), BuySell.Buy);
					this.portfolio.update(tradeAction);
					Logger.emit(this.getClass().getSimpleName(), "Closed Short Position at " + currentPrice + " Net: " + this.portfolio.netWorth(marketData));
				}
			} else if (portfolio.getPosition().getNotional() > 0){
				if(signal < lag){
					TradeAction tradeAction = new TradeAction(null, Math.abs(portfolio.getPosition().getNotional()),
							marketData.getLast().getCandle().getClose(), BuySell.Sell);
					this.portfolio.update(tradeAction);
					Logger.emit(this.getClass().getSimpleName(), "Closed Long Position at " + currentPrice + " Net: " + this.portfolio.netWorth(marketData));
				}
			} else {
				//Upward Pressure
				if(prevLongLag > prevLongSignal && longLag < longSignal){
					Double amount = this.getPortfolio().getBalance() * notionalRatio;
					//If previously holding a short position
//				if(portfolio.getPosition().getNotional() < 0){
//					amount += Math.abs(portfolio.getPosition().getNotional());
//				}

					TradeAction tradeAction = new TradeAction(null, amount,
							marketData.getLast().getCandle().getClose(), BuySell.Buy);
					this.getPortfolio().update(tradeAction);
					Logger.emit(this.getClass().getSimpleName(), "Opened Long Position at " + currentPrice + " Net: " + this.portfolio.netWorth(marketData));
				}

				//Downward Pressure
				else if (prevLongLag < prevLongSignal && longLag > longSignal){
					Double amount = this.getPortfolio().getBalance() * notionalRatio;
					//If previously holding a long position
//				if(portfolio.getPosition().getNotional() > 0){
//					amount += Math.abs(portfolio.getPosition().getNotional());
//				}
					TradeAction tradeAction = new TradeAction(null, amount,
							marketData.getLast().getCandle().getClose(), BuySell.Sell);
					this.getPortfolio().update(tradeAction);
					Logger.emit(this.getClass().getSimpleName(), "Opened Long Position at " + currentPrice + " Net: " + this.portfolio.netWorth(marketData));

				}
			}
		}
	}
}
