import requests
import pandas as pd
from bs4 import BeautifulSoup
from selenium import webdriver
from selenium.webdriver.chrome.service import Service
from dateutil.relativedelta import relativedelta
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime
import time
import csv
import os

options = webdriver.ChromeOptions()
options.add_argument('--headless')
options.add_argument('--no-sandbox')
options.add_argument('--disable-dev-shm-usage')

folder_path='stock_data_files'
if not os.path.exists(folder_path):
  os.makedirs(folder_path)

# FILTER 1
def fetch_and_filter_tickers(url, options):
    driver = webdriver.Chrome(service=Service(), options=options)
    driver.get(url)
    time.sleep(5)

    soup = BeautifulSoup(driver.page_source, 'html.parser')
    driver.quit()

    tickers = []
    dropdown = soup.find("select", id="Code")
    if dropdown:
        options = dropdown.find_all("option")
        for option in options:
            ticker = option.get("value")
            if ticker and ticker.isalpha():
                tickers.append(ticker.strip())
    return tickers


# FILTER 2
def check_last_date(ticker):
    file_name = os.path.join(folder_path, f"{ticker}.csv")
    if not os.path.exists(file_name):
        return None

    with open(file_name, "r", encoding="utf-8") as file:
        reader = csv.DictReader(file)
        dates = [row["Датум"] for row in reader if row["Датум"]]
        if dates:
            return max(dates, key=lambda d: datetime.strptime(d, "%d.%m.%Y"))
    return None


# FILTER 3
def fetch_and_fill_data(ticker, last_date):
    base_url = 'https://www.mse.mk/mk/stats/symbolhistory/ALK'
    headers = {"Content-Type": "application/json"}
    today = datetime.now()
    dataDF = []

    if last_date:
        last_date_obj = datetime.strptime(last_date, "%d.%m.%Y")
        from_date = last_date_obj + relativedelta(days=1)
    else:
        from_date = today - relativedelta(years=10)

    while from_date <= today:
        to_date = min(from_date + relativedelta(years=1) - relativedelta(days=1), today)
        from_date_str = from_date.strftime("%d.%m.%Y")
        to_date_str = to_date.strftime("%d.%m.%Y")

        data = {"FromDate": from_date_str, "ToDate": to_date_str, "Code": ticker}
        response = requests.post(base_url, headers=headers, json=data)
        soup = BeautifulSoup(response.text, 'html.parser')

        rows = soup.select("#resultsTable tr")
        for row in rows:
            tds = row.find_all("td")
            if tds:
                row_data = {
                    "Датум": tds[0].text.strip() if tds[0] else None,
                    "Цена на последна трансакција": tds[1].text.strip() if tds[1] else None,
                    "Мак.": tds[2].text.strip() if tds[2] else None,
                    "Мин.": tds[3].text.strip() if tds[3] else None,
                    "Просечна цена": tds[4].text.strip() if tds[4] else None,
                    "%пром.": tds[5].text.strip() if tds[5] else None,
                    "Количина": tds[6].text.strip() if tds[6] else None,
                    "Промет во БЕСТ во денари": tds[7].text.strip() if tds[7] else None,
                    "Вкупен промет во денари": tds[8].text.strip() if tds[8] else None,
                }
                dataDF.append(row_data)

        from_date = to_date + relativedelta(days=1)

    df = pd.DataFrame(dataDF)
    file_name = os.path.join(folder_path, f"{ticker}.csv")
    if os.path.exists(file_name):
        df.to_csv(file_name, mode="a", header=False, index=False)
    else:
        df.to_csv(file_name, index=False)
    return df


def process_ticker(ticker):
    last_date = check_last_date(ticker)
    fetch_and_fill_data(ticker, last_date)


def pipeline(url):
    start_time = time.time()

    tickers = fetch_and_filter_tickers(url, options)

    with ThreadPoolExecutor(max_workers=5) as executor:
        executor.map(process_ticker, tickers)

    end_time = time.time()
    elapsed_time = end_time - start_time

    print(f"Time taken to populate the database: {elapsed_time:.2f} seconds")

url = 'https://www.mse.mk/mk/stats/symbolhistory/ALK'
pipeline(url)