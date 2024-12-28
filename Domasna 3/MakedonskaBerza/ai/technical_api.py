import numpy as np
import pandas as pd
from flask import Flask, jsonify, request

app=Flask(__name__)

def compute_rsi(data,lookback=14):
    changes = data['close'].diff()
    gains = (changes.where(changes > 0, 0)).rolling(window=lookback).mean()
    losses = (-changes.where(changes < 0, 0)).rolling(window=lookback).mean()
    relative_strength = gains / losses
    rsi_values = 100 - (100 / (1 + relative_strength))
    return rsi_values

def compute_macd(data, fast=12, slow=26, signal=9):
    fast_ema = data['close'].ewm(span=fast, min_periods=fast).mean()
    slow_ema = data['close'].ewm(span=slow, min_periods=slow).mean()
    macd_line = fast_ema - slow_ema
    macd_signal_line = macd_line.ewm(span=signal, min_periods=signal).mean()
    macd_histogram = macd_line - macd_signal_line
    return macd_line, macd_signal_line, macd_histogram

def stochastic_oscillator(data, window=14):
    lowest_low = data['low'].rolling(window=window).min()
    highest_high = data['high'].rolling(window=window).max()
    stochastic_values = 100 * ((data['close'] - lowest_low) / (highest_high - lowest_low))
    return stochastic_values

def compute_cci(data, window=20):
    typical_price = (data['high'] + data['low'] + data['close']) / 3
    moving_avg = typical_price.rolling(window=window).mean()
    mean_dev = (typical_price - moving_avg).abs().rolling(window=window).mean()
    cci_values = (typical_price - moving_avg) / (0.015 * mean_dev)
    return cci_values

def compute_atr(data, window=14):
    data['HL'] = data['high'] - data['low']
    data['HPC'] = abs(data['high'] - data['close'].shift(1))
    data['LPC'] = abs(data['low'] - data['close'].shift(1))
    data['TrueRange'] = data[['HL', 'HPC', 'LPC']].max(axis=1)
    atr_values = data['TrueRange'].rolling(window=window).mean()
    return atr_values

def calculate_simple_moving_average(dataframe, period=10):
    return dataframe['close'].rolling(window=period).mean()

def calculate_exponential_moving_average(dataframe, smoothing=10):
    return dataframe['close'].ewm(span=smoothing, adjust=False).mean()

def calculate_weighted_moving_average(dataframe, period=10):
    weights = np.arange(1, period + 1)
    return dataframe['close'].rolling(window=period).apply(
        lambda prices: np.dot(prices, weights) / weights.sum(), raw=True
    )

def calculate_hull_moving_average(dataframe, length=14):
    half_length = length // 2
    sqrt_length = int(np.sqrt(length))
    wma_half = calculate_weighted_moving_average(dataframe, period=half_length)
    wma_full = calculate_weighted_moving_average(dataframe, period=length)
    hma = calculate_weighted_moving_average(
        pd.DataFrame({'close': 2 * wma_half - wma_full}),
        period=sqrt_length
    )
    return hma[0]


def calculate_volume_weighted_average_price(dataframe):
    vwap = (dataframe['close'] * dataframe['volume']).cumsum() / dataframe['volume'].cumsum()
    return vwap

def generate_trade_signal(dataframe, period=14):
    # Извлекување на последниот запис од податоците
    recent_data = dataframe.iloc[-1]

    # RSI услови
    relative_strength_index = recent_data[f'RSI_{period}']
    if relative_strength_index < 30:
        rsi_action = 'Buy'
    elif relative_strength_index > 70:
        rsi_action = 'Sell'
    else:
        rsi_action = 'Hold'

    # MACD услови
    macd_histogram = recent_data[f'MACD_hist_{period}']
    if macd_histogram > 0:
        macd_action = 'Buy'
    elif macd_histogram < 0:
        macd_action = 'Sell'
    else:
        macd_action = 'Hold'

    # Stochastic услови
    stochastic_value = recent_data[f'Stochastic_{period}']
    if stochastic_value < 20:
        stochastic_action = 'Buy'
    elif stochastic_value > 80:
        stochastic_action = 'Sell'
    else:
        stochastic_action = 'Hold'

    # CCI услови
    commodity_channel_index = recent_data[f'CCI_{period}']
    if commodity_channel_index > 100:
        cci_action = 'Sell'
    elif commodity_channel_index < -100:
        cci_action = 'Buy'
    else:
        cci_action = 'Hold'

    # ATR услови
    average_true_range = recent_data[f'ATR_{period}']
    if average_true_range > 1.5:
        atr_action = 'Sell'
    else:
        atr_action = 'Buy'

    # Moving averages услови
    short_moving_avg = recent_data[f'SMA_{period}']
    long_moving_avg = recent_data['SMA_50']
    exponential_avg = recent_data[f'EMA_{period}']
    weighted_avg = recent_data[f'WMA_{period}']
    hull_avg = recent_data[f'HMA_{period}']
    volume_weighted_avg = recent_data[f'VWAP_{period}']

    if (
            short_moving_avg > long_moving_avg
            and exponential_avg > long_moving_avg
            and weighted_avg > long_moving_avg
            and hull_avg > long_moving_avg
            and volume_weighted_avg > long_moving_avg
    ):
        trend_action = 'Buy'
    else:
        trend_action = 'Sell'

    # Финален сигнал
    if rsi_action == 'Buy' and macd_action == 'Buy' and trend_action == 'Buy':
        overall_signal = 'Buy'
    elif rsi_action == 'Sell' and macd_action == 'Sell' and trend_action == 'Sell':
        overall_signal = 'Sell'
    else:
        overall_signal = 'Hold'

    return overall_signal


#Route
@app.route('/generate_signal', methods=['POST'])
def api_trade_signal():
    incoming_data = request.get_json()
    dataset = pd.DataFrame(incoming_data)
    analysis_windows = [5, 10, 14, 20, 50]

    for window in analysis_windows:
        dataset[f'RSI_{window}'] = compute_rsi(dataset, lookback=window)
        dataset[f'MACD_line_{window}'], dataset[f'MACD_signal_{window}'], dataset[f'MACD_hist_{window}'] = compute_macd(dataset)
        dataset[f'Stoch_{window}'] = stochastic_oscillator(dataset, window=window)
        dataset[f'CCI_index_{window}'] = compute_cci(dataset, window=window)
        dataset[f'ATR_value_{window}'] = compute_atr(dataset, window=window)
        dataset[f'SMA_{window}'] = calculate_simple_moving_average(dataset, period=window)
        dataset[f'EMA_{window}'] = calculate_exponential_moving_average(dataset, smoothing=window)
        dataset[f'WMA_{window}'] = calculate_weighted_moving_average(dataset, period=window)
        dataset[f'HMA_{window}'] = calculate_hull_moving_average(dataset, length=window)
        dataset[f'VWAP_{window}'] = calculate_volume_weighted_average_price(dataset)

    signal_result = generate_trade_signal(dataset)

    return jsonify({"trade_signal": signal_result})



if __name__ == "__main__":
    app.run(debug=True)
