import os
import cv2
import pathlib
import requests
from datetime import datetime


class ChangeDetection:
    result_prev = []
    #HOST = 'http://127.0.0.1:8000'  # local
    HOST = 'https://jerryzoo.pythonanywhere.com'  # pythonanywhere
    username = 'admin'
    password = 'admin'
    token = ''
    title = ''
    text = ''

    # 물건별 가격 매핑
    price_map = {
    "bicycle": 250000,
    "car": 20000000,
    "motorcycle": 4000000,
    "airplane": 15000000000,
    "bus": 50000000,
    "train": 300000000,
    "truck": 80000000,
    "boat": 20000000,
    "traffic light": 150000,
    "fire hydrant": 50000,
    "stop sign": 10000,
    "parking meter": 500000,
    "bench": 100000,
    "bird": 20000,
    "cat": 50000,
    "dog": 80000,
    "horse": 3000000,
    "sheep": 200000,
    "cow": 1500000,
    "elephant": 500000000,
    "bear": 700000000,
    "zebra": 10000000,
    "giraffe": 200000000,
    "backpack": 70000,
    "umbrella": 15000,
    "handbag": 80000,
    "tie": 20000,
    "suitcase": 120000,
    "frisbee": 15000,
    "skis": 250000,
    "snowboard": 200000,
    "sports ball": 30000,
    "kite": 10000,
    "baseball bat": 40000,
    "baseball glove": 50000,
    "skateboard": 100000,
    "surfboard": 300000,
    "tennis racket": 200000,
    "bottle": 2000,
    "wine glass": 10000,
    "cup": 5000,
    "fork": 3000,
    "knife": 5000,
    "spoon": 2000,
    "bowl": 10000,
    "banana": 1000,
    "apple": 2000,
    "sandwich": 5000,
    "orange": 2000,
    "broccoli": 3000,
    "carrot": 2000,
    "hot dog": 4000,
    "pizza": 15000,
    "donut": 3000,
    "cake": 20000,
    "chair": 50000,
    "couch": 300000,
    "potted plant": 20000,
    "bed": 400000,
    "dining table": 200000,
    "toilet": 150000,
    "tv": 1000000,
    "laptop": 1500000,
    "mouse": 30000,
    "remote": 20000,
    "keyboard": 50000,
    "cell phone": 800000,
    "microwave": 200000,
    "oven": 500000,
    "toaster": 300000,
    "sink": 100000,
    "refrigerator": 1200000,
    "book": 10000,
    "clock": 30000,
    "vase": 50000,
    "scissors": 5000,
    "teddy bear": 20000,
    "hair drier": 50000,
    "toothbrush": 3000,
}


    def __init__(self, names):
        self.result_prev = [0 for i in range(len(names))]
        self.names = names
        print(self.names)

        res = requests.post(self.HOST + '/api-token-auth/', {
            'username': self.username,
            'password': self.password
        })
        res.raise_for_status()
        self.token = res.json()['token']
        print(self.token)

    def add(self, names, detected_current, save_dir, image):
        self.title = ''
        self.text = ''
        change_flag = 0

        # 이전 탐지 결과와 현재 결과 비교
        i = 0
        while i < len(self.result_prev):

            # 새로 탐지된 물체에 대해 처리
            if self.result_prev[i] == 0 and detected_current[i] == 1:
                item_name = names[i]
                if item_name in self.price_map:  # price_map에 있는 물건만 처리
                    change_flag = 1
                    price = self.price_map.get(item_name, "가격 정보 없음")
                    self.title = item_name
                    self.text += f"{item_name} - {price}원"
            i += 1

        self.result_prev = detected_current[:]

        if change_flag == 1:
            self.send(save_dir, image)

    def send(self, save_dir, image):
        now = datetime.now()
        now.isoformat()

        today = datetime.now()
        save_path = os.getcwd() / save_dir / 'detected' / str(today.year) / \
            str(today.month) / str(today.day)
        pathlib.Path(save_path).mkdir(parents=True, exist_ok=True)

        full_path = save_path / \
            '{0}-{1}-{2}-{3}.jpg'.format(today.hour,
                                         today.minute, today.second, today.microsecond)

        dst = cv2.resize(image, dsize=(320, 240), interpolation=cv2.INTER_AREA)
        cv2.imwrite(full_path, dst)

        headers = {'Authorization': 'Token ' +
                   self.token, 'Accept': 'application/json'}

        data = {
            'author': 1,
            'title': self.title,
            'text': self.text,
            'created_date': now,
            'published_date': now
        }

        file = {'image': open(full_path, 'rb')}
        res = requests.post(self.HOST + '/api_root/Post/',
                            data=data, files=file, headers=headers)
        print(res)
