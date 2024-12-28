from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List
import pandas as pd
import numpy as np
from statsmodels.tsa.arima.model import ARIMA
from datetime import datetime
from ta import trend, momentum, volatility
import ta

app = FastAPI()


# Define a model for historical data items
class HistoricalDataItem(BaseModel):
    date: str  # Expect date as 'YYYY-MM-DD'
    last_transaction_price: float
    max_price: float
    min_price: float
    average_price: float
    quantity: int

# Define a model for the list of historical data
class HistoricalData(BaseModel):
    data: List[HistoricalDataItem]


# Calculate indicators
def calculate_indicators(historical_data: pd.DataFrame, indicator_id: int, days: int):
    # indicators = ["RSI", "MACD", "Stochastic", "CCI", "MOM", "SMA", "EMA", "WMA", "HMA", "VWAP"]

    historical_data['close'] = historical_data['last_transaction_price']
    historical_data['high'] = historical_data['max_price']
    historical_data['low'] = historical_data['min_price']
    historical_data['volume'] = historical_data['quantity']

    if indicator_id == 0:  # RSI
        historical_data['RSI'] = ta.momentum.RSIIndicator(historical_data['close'], window=days).rsi()
    elif indicator_id == 1:  # MACD
        macd = ta.trend.MACD(historical_data['close'], window_slow=26, window_fast=12, window_sign=days)
        historical_data['MACD'] = macd.macd()
    elif indicator_id == 2:  # Stochastic Oscillator
        stoch = ta.momentum.StochasticOscillator(
            high=historical_data['high'], low=historical_data['low'], close=historical_data['close'], window=days
        )
        historical_data['Stochastic'] = stoch.stoch()
    elif indicator_id == 3:  # CCI
        historical_data['CCI'] = ta.trend.CCIIndicator(
            high=historical_data['high'], low=historical_data['low'], close=historical_data['close'], window=days
        ).cci()
    elif indicator_id == 4:  # Momentum (MOM)
        historical_data['MOM'] = ta.momentum.WilliamsRIndicator(
            high=historical_data['high'], low=historical_data['low'], close=historical_data['close'], lbp=days
        ).williams_r()
    elif indicator_id == 5:  # SMA
        historical_data['SMA'] = historical_data['close'].rolling(window=days).mean()
    elif indicator_id == 6:  # EMA
        historical_data['EMA'] = historical_data['close'].ewm(span=days, adjust=False).mean()
    elif indicator_id == 7:  # WMA
        historical_data['WMA'] = ta.trend.WMAIndicator(historical_data['close'], window=days).wma()
    elif indicator_id == 8:  # HMA
        def hma(series, period):
            wma1 = series.rolling(window=period // 2).mean()
            wma2 = series.rolling(window=period).mean()
            diff = 2 * wma1 - wma2
            return diff.rolling(window=int(period**0.5)).mean()

        historical_data['HMA'] = hma(historical_data['close'], days)
    elif indicator_id == 9:  # VWAP
        vwap = (historical_data['close'] * historical_data['volume']).cumsum() / historical_data['volume'].cumsum()
        historical_data['VWAP'] = vwap
    else:
        print("Невалиден индекс на индикатор!")

    return historical_data


# Function to filter data based on the selected timeframe
def filter_data_by_timeframe(historical_data: pd.DataFrame, timeframe: str) -> pd.DataFrame:
    # Convert 'date' to datetime
    historical_data['date'] = pd.to_datetime(historical_data['date'])

    if timeframe == '1_day':
        # Use only the last day's data
        historical_data = historical_data.tail(1)
    elif timeframe == '1_week':
        # Use data from the last 7 days
        historical_data = historical_data.tail(7)
    elif timeframe == '1_month':
        # Use data from the last 30 days
        historical_data = historical_data.tail(30)
    else:
        raise HTTPException(status_code=400, detail="Invalid timeframe. Valid options: 1_day, 1_week, 1_month")

    return historical_data


# Presmetaj indikator za razlicni vremenski ramki
def calculate_indicators_for_timeframe(historical_data: pd.DataFrame, indicator_id: int, days: int, timeframe: str):
    # Filter data for selected timeframe
    historical_data = filter_data_by_timeframe(historical_data, timeframe)

    # Call the function to calculate the selected indicator for the timeframe
    return calculate_indicators(historical_data, indicator_id, days)


# Prediction function using ARIMA
def predict_next_month_price(historical_data: pd.DataFrame) -> float:
    # Set the date as the index
    historical_data['date'] = pd.to_datetime(historical_data['date'])
    historical_data.set_index('date', inplace=True)

    # Apply ARIMA model
    model = ARIMA(historical_data['average_price'], order=(5, 1, 0))  # Adjust order if needed
    model_fit = model.fit()

    # Forecast for 30 days (next month)
    forecast = model_fit.forecast(steps=30)

    # Return the mean forecast price for the next month
    return forecast.mean()


# Generate buy/sell signals
def generate_signals(historical_data: pd.DataFrame, indicator_id: int) -> pd.DataFrame:
    if indicator_id == 0:  # RSI
        # Generating Buy/Sell signals based on RSI
        historical_data['buy_signal'] = historical_data['RSI'] < 30
        historical_data['sell_signal'] = historical_data['RSI'] > 70
    elif indicator_id == 1:  # MACD
        # Generating Buy/Sell signals based on MACD
        historical_data['buy_signal'] = historical_data['MACD'] > historical_data['MACD_signal']
        historical_data['sell_signal'] = historical_data['MACD'] < historical_data['MACD_signal']
    elif indicator_id == 2:  # Stochastic Oscillator
        # Generating Buy/Sell signals based on Stochastic
        historical_data['buy_signal'] = historical_data['Stochastic'] < 20
        historical_data['sell_signal'] = historical_data['Stochastic'] > 80
    elif indicator_id == 3:  # CCI
        # Generating Buy/Sell signals based on CCI
        historical_data['buy_signal'] = historical_data['CCI'] > 100
        historical_data['sell_signal'] = historical_data['CCI'] < -100
    elif indicator_id == 4:  # Momentum
        # Generating Buy/Sell signals based on MOM
        historical_data['buy_signal'] = historical_data['MOM'] > 0
        historical_data['sell_signal'] = historical_data['MOM'] < 0
    elif indicator_id == 5:  # SMA
        # Generating Buy/Sell signals based on SMA
        historical_data['buy_signal'] = historical_data['close'] > historical_data['SMA']
        historical_data['sell_signal'] = historical_data['close'] < historical_data['SMA']
    elif indicator_id == 6:  # EMA
        # Generating Buy/Sell signals based on EMA
        historical_data['buy_signal'] = historical_data['close'] > historical_data['EMA']
        historical_data['sell_signal'] = historical_data['close'] < historical_data['EMA']
    elif indicator_id == 7:  # WMA
        # Generating Buy/Sell signals based on WMA
        historical_data['buy_signal'] = historical_data['close'] > historical_data['WMA']
        historical_data['sell_signal'] = historical_data['close'] < historical_data['WMA']
    elif indicator_id == 8:  # HMA
        # Generating Buy/Sell signals based on HMA
        historical_data['buy_signal'] = historical_data['close'] > historical_data['HMA']
        historical_data['sell_signal'] = historical_data['close'] < historical_data['HMA']
    elif indicator_id == 9:  # VWAP
        # Generating Buy/Sell signals based on VWAP
        historical_data['buy_signal'] = historical_data['close'] > historical_data['VWAP']
        historical_data['sell_signal'] = historical_data['close'] < historical_data['VWAP']
    else:
        print("Невалиден индекс на индикатор!")

    # Additional signal columns (e.g., MA based signals)
    # MA Buy/Sell signals can be generated for the indicators based on simple moving average crossovers
    historical_data['ma_buy_signal'] = historical_data['close'] > historical_data['SMA']
    historical_data['ma_sell_signal'] = historical_data['close'] < historical_data['SMA']

    return historical_data


# Define an endpoint for predicting the stock price
# TODO: send the selected indicator_id and days[1, 7, 30] from the user
@app.post("/predict-indicators-and-signals/")
async def predict_indicators_and_signals_endpoint(historical_data: HistoricalData, indicator_id: int, days: int):
    try:
        data = pd.DataFrame([{
            'date': item.date,
            'average_price': item.average_price,
            'last_transaction_price': item.last_transaction_price,
            'max_price': item.max_price,
            'min_price': item.min_price,
            'quantity': item.quantity
        } for item in historical_data.data])

        indicators = ["RSI", "MACD", "Stochastic", "CCI", "MOM", "SMA", "EMA", "WMA", "HMA", "VWAP"]

        # Calculate the indicators
        data = calculate_indicators(data, indicator_id=indicator_id, days=days)
        indicator_value = data.tail(1)[indicators[indicator_id]].values[0]

        # Get signals for different timeframes
        signals_day = generate_signals(filter_data_by_timeframe(data, "1_day"), indicator_id)
        signals_week = generate_signals(filter_data_by_timeframe(data, "1_week"), indicator_id)
        signals_month = generate_signals(filter_data_by_timeframe(data, "1_month"), indicator_id)

        return {
            "indicator_value": indicator_value,
            "signals_1_day": signals_day[['buy_signal', 'sell_signal']],  # .to_dict(orient="records"),
            "signals_1_week": signals_week[['buy_signal', 'sell_signal']],  # .to_dict(orient="records"),
            "signals_1_month": signals_month[['buy_signal', 'sell_signal']]  # .to_dict(orient="records")
        }
    except Exception as e:
        # Handle exceptions during prediction
        print(f"Error during prediction: {str(e)}")
        return {"error": str(e)}  # Return an error message in the response


@app.post("/predict-next-month-price/")
async def predict_next_month_price_endpoint(historical_data: HistoricalData):
    # Convert the list of data to a DataFrame
    try:
        data = pd.DataFrame([item.dict() for item in historical_data.data])
        predicted_price = predict_next_month_price(data)
        return {"predicted_next_month_price": predicted_price}
    except Exception as e:
        # Handle exceptions during prediction
        print(f"Error during prediction: {str(e)}")
        return {"error": str(e)}  # Return an error message in the response

# Run the app with Uvicorn (Python ASGI server)
# Command to run: uvicorn prediction_api:app --reload
