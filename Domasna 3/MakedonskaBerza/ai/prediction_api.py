# import numpy as np
# import pandas as pd
# import tensorflow as tf
#
#
# print(tf.keras.__version__)
#
# from tensorflow.keras.models import Sequential
#
# from tensorflow.keras.layers import LSTM, Dense, Dropout
# from sklearn.preprocessing import MinMaxScaler
# from fastapi import FastAPI, HTTPException
# from typing import List
# from pydantic import BaseModel
#
# app = FastAPI()
#
#
# # Define a model for historical data items
# class HistoricalDataItem(BaseModel):
#     date: str  # Expect date as 'YYYY-MM-DD'
#     average_price: float
#
#
# # Define a model for the list of historical data
# class HistoricalData(BaseModel):
#     data: List[HistoricalDataItem]
#
#
# # Helper function to prepare data for LSTM
# def prepare_data_for_lstm(data: pd.DataFrame, time_steps=30):
#     scaler = MinMaxScaler(feature_range=(0, 1))
#     data_scaled = scaler.fit_transform(data['average_price'].values.reshape(-1, 1))
#
#     X, y = [], []
#     for i in range(time_steps, len(data)):
#         X.append(data_scaled[i - time_steps:i, 0])  # Past 'time_steps' values
#         y.append(data_scaled[i, 0])  # Current value as the label
#
#     X, y = np.array(X), np.array(y)
#     X = X.reshape(X.shape[0], X.shape[1], 1)  # Reshape for LSTM [samples, time_steps, features]
#
#     return X, y, scaler
#
#
# # LSTM model definition
# def create_lstm_model(input_shape):
#     model = Sequential()
#     model.add(LSTM(units=50, return_sequences=False, input_shape=input_shape))
#     model.add(Dense(units=1))  # Output layer
#     model.compile(optimizer='adam', loss='mean_squared_error')
#     return model
#
#
# # Prediction function using LSTM
# def predict_next_month_price(historical_data: pd.DataFrame) -> float:
#     # Set the date as the index
#     historical_data['date'] = pd.to_datetime(historical_data['date'])
#     historical_data.set_index('date', inplace=True)
#
#     historical_data = historical_data.dropna()
#
#     if len(historical_data) < 30:
#         raise ValueError("Not enough data to make a reliable prediction.")
#
#     # Prepare data for LSTM
#     X, y, scaler = prepare_data_for_lstm(historical_data)
#
#     # Create and train the LSTM model
#     model = create_lstm_model((X.shape[1], 1))
#     model.fit(X, y, epochs=10, batch_size=32, verbose=1)
#
#     # Predict next month (forecast for 30 days)
#     last_30_days = X[-1].reshape(1, 30, 1)
#     forecast = model.predict(last_30_days)
#
#     # Inverse scale the forecasted value
#     predicted_price = scaler.inverse_transform(forecast)
#
#     return predicted_price[0][0]
#
#
# @app.post("/predict-next-month-price/")
# async def predict_next_month_price_endpoint(historical_data: HistoricalData):
#     # Convert the list of data to a DataFrame
#     try:
#         data = pd.DataFrame([item.dict() for item in historical_data.data])
#         predicted_price = predict_next_month_price(data)
#
#         return {
#             "predicted_next_month_price": predicted_price
#         }
#     except Exception as e:
#         raise HTTPException(status_code=500, detail=str(e))
#

# Run the app with Uvicorn (Python ASGI server)
# Command to run: uvicorn prediction_api:app --reload



from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List
import pandas as pd
from statsmodels.tsa.arima.model import ARIMA
from datetime import datetime

app = FastAPI()


# Define a model for historical data items
class HistoricalDataItem(BaseModel):
    date: str  # Expect date as 'YYYY-MM-DD'
    average_price: float


# Define a model for the list of historical data
class HistoricalData(BaseModel):
    data: List[HistoricalDataItem]


# Prediction function using ARIMA
def predict_next_month_price(historical_data: pd.DataFrame) -> float:
    # Set the date as the index
    historical_data['date'] = pd.to_datetime(historical_data['date'])
    historical_data.set_index('date', inplace=True)

    historical_data = historical_data.dropna()

    if len(historical_data) < 30:
        raise ValueError("Not enough data to make a reliable prediction.")

    # Apply ARIMA model
    model = ARIMA(historical_data['average_price'], order=(5, 1, 0))  # Adjust order if needed
    model_fit = model.fit()

    # Forecast for 30 days (next month)
    forecast = model_fit.forecast(steps=30)

    # Return the mean forecast price for the next month
    return forecast.mean()


@app.post("/predict-next-month-price/")
async def predict_next_month_price_endpoint(historical_data: HistoricalData):
    # Convert the list of data to a DataFrame
    try:
        data = pd.DataFrame([item.dict() for item in historical_data.data])
        predicted_price = predict_next_month_price(data)

        return {
            "predicted_next_month_price": predicted_price
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# Run the app with Uvicorn (Python ASGI server)
# Command to run: uvicorn prediction_api:app --reload
