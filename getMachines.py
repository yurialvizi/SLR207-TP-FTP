import random
import requests

url = "https://tp.telecom-paris.fr/ajax.php"
file_name = "myftpclient/src/main/resources/machines.txt"

cookies = {
    'AMP_MKTG_075dfa7287': 'JTdCJTdE',
    'AMP_075dfa7287': 'JTdCJTIyZGV2aWNlSWQlMjIlM0ElMjJlYTQ4ZDBkNi1lNTQ4LTQ3ODgtYmQ4Ny1jNGZiNmM2MTk1YzUlMjIlMkMlMjJzZXNzaW9uSWQlMjIlM0ExNzE2Mjc4MTg2MjA4JTJDJTIyb3B0T3V0JTIyJTNBZmFsc2UlMkMlMjJsYXN0RXZlbnRUaW1lJTIyJTNBMTcxNjI3ODQ2NTk5MCU3RA==',
}

headers = {
    'accept': 'application/json, text/javascript, */*; q=0.01',
    'accept-language': 'pt-PT,pt;q=0.9,en-US;q=0.8,en;q=0.7',
    # 'cookie': 'AMP_MKTG_075dfa7287=JTdCJTdE; AMP_075dfa7287=JTdCJTIyZGV2aWNlSWQlMjIlM0ElMjJlYTQ4ZDBkNi1lNTQ4LTQ3ODgtYmQ4Ny1jNGZiNmM2MTk1YzUlMjIlMkMlMjJzZXNzaW9uSWQlMjIlM0ExNzE2Mjc4MTg2MjA4JTJDJTIyb3B0T3V0JTIyJTNBZmFsc2UlMkMlMjJsYXN0RXZlbnRUaW1lJTIyJTNBMTcxNjI3ODQ2NTk5MCU3RA==',
    'priority': 'u=1, i',
    'referer': 'https://tp.telecom-paris.fr/',
    'sec-ch-ua': '"Not/A)Brand";v="8", "Chromium";v="126", "Google Chrome";v="126"',
    'sec-ch-ua-mobile': '?0',
    'sec-ch-ua-platform': '"Linux"',
    'sec-fetch-dest': 'empty',
    'sec-fetch-mode': 'cors',
    'sec-fetch-site': 'same-origin',
    'user-agent': 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36',
    'x-requested-with': 'XMLHttpRequest',
}

params = {
    '_': '1718964587269',
}

def getAvailableMachines():
    response = requests.get(url, params=params, cookies=cookies, headers=headers)
    data = response.json()["data"]
    machines = [item[0] for item in data if item[1] == True and item[2:] == [0,0,0]]
    ## Shuffle the machines
    random.shuffle(machines)
    file = open(file_name, "w")
    for machine in machines:
        file.write(machine + "\n")
    file.close()


def main():
    getAvailableMachines()

if __name__ == "__main__":
    main()