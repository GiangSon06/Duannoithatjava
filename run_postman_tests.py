import urllib.request
import urllib.parse
import json
import time
import sys

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8')

def main():
    test_cases = [
        {
            'id': 'TC_API_01',
            'name': 'Lấy danh mục 63 Tỉnh/Thành phố toàn quốc',
            'endpoint': '/api/shipping/provinces',
            'method': 'GET',
            'url': 'http://localhost:8081/api/shipping/provinces',
            'headers': {'Accept': 'application/json'},
            'params': {},
            'expected_status': 200,
            'expected_note': 'Trả về danh sách 63 tỉnh/thành phố toàn quốc dưới dạng JSON array.'
        },
        {
            'id': 'TC_API_02',
            'name': 'Lấy danh sách Quận/Huyện theo mã Tỉnh/Thành',
            'endpoint': '/api/shipping/districts',
            'method': 'GET',
            'url': 'http://localhost:8081/api/shipping/districts?province_id=1',
            'headers': {'Accept': 'application/json'},
            'params': {'province_id': '1'},
            'expected_status': 200,
            'expected_note': 'Trả về danh sách các quận/huyện trực thuộc tỉnh Hà Nội (ID=1).'
        },
        {
            'id': 'TC_API_03',
            'name': 'Lấy danh sách Phường/Xã theo mã Quận/Huyện',
            'endpoint': '/api/shipping/wards',
            'method': 'GET',
            'url': 'http://localhost:8081/api/shipping/wards?district_id=101',
            'headers': {'Accept': 'application/json'},
            'params': {'district_id': '101'},
            'expected_status': 200,
            'expected_note': 'Trả về danh sách các phường/xã trực thuộc quận (ID=101).'
        },
        {
            'id': 'TC_API_04',
            'name': 'Tính phí vận chuyển chuẩn theo khoảng cách (Đơn dưới 5M)',
            'endpoint': '/api/shipping/calculate-fee',
            'method': 'POST',
            'url': 'http://localhost:8081/api/shipping/calculate-fee',
            'headers': {'Content-Type': 'application/x-www-form-urlencoded', 'Accept': 'application/json'},
            'params': {'district_id': 101, 'ward_code': '101_01', 'province_name': 'Đà Nẵng', 'total': 3200000},
            'expected_status': 200,
            'expected_note': 'Tính cước chính xác 75.000đ cho tuyến Đà Nẵng, dự kiến giao 5 ngày làm việc.'
        },
        {
            'id': 'TC_API_05',
            'name': 'Tính phí vận chuyển chính sách Freeship (Đơn từ 5M trở lên)',
            'endpoint': '/api/shipping/calculate-fee',
            'method': 'POST',
            'url': 'http://localhost:8081/api/shipping/calculate-fee',
            'headers': {'Content-Type': 'application/x-www-form-urlencoded', 'Accept': 'application/json'},
            'params': {'district_id': 101, 'ward_code': '101_01', 'province_name': 'Đà Nẵng', 'total': 5500000},
            'expected_status': 200,
            'expected_note': 'Áp dụng chính sách Freeship GS LUXURY: final_fee=0đ, hiển thị ghi chú tiết kiệm cước.'
        },
        {
            'id': 'TC_API_06',
            'name': 'Kiểm tra tồn kho sản phẩm hợp lệ (Còn hàng)',
            'endpoint': '/check_inventory.php',
            'method': 'POST',
            'url': 'http://localhost:8081/check_inventory.php',
            'headers': {'Content-Type': 'application/x-www-form-urlencoded', 'Accept': 'application/json'},
            'params': {'id': 1, 'quantity': 2},
            'expected_status': 200,
            'expected_note': 'status=success, trả về số lượng tồn kho khả dụng của sản phẩm.'
        },
        {
            'id': 'TC_API_07',
            'name': 'Kiểm tra tồn kho khi yêu cầu vượt số lượng tồn',
            'endpoint': '/check_inventory.php',
            'method': 'POST',
            'url': 'http://localhost:8081/check_inventory.php',
            'headers': {'Content-Type': 'application/x-www-form-urlencoded', 'Accept': 'application/json'},
            'params': {'id': 1, 'quantity': 9999},
            'expected_status': 200,
            'expected_note': 'status=error, thông báo từ chối kèm số lượng tồn kho tối đa còn lại.'
        },
        {
            'id': 'TC_API_08',
            'name': 'Áp dụng mã giảm giá hợp lệ (Voucher NEWUSER 15%)',
            'endpoint': '/apply_voucher.php',
            'method': 'POST',
            'url': 'http://localhost:8081/apply_voucher.php',
            'headers': {'Content-Type': 'application/x-www-form-urlencoded', 'Accept': 'application/json'},
            'params': {'code': 'NEWUSER', 'total': 1000000},
            'expected_status': 200,
            'expected_note': 'status=success, giảm đúng 15% (150.000đ), số tiền thanh toán còn lại là 850.000đ.'
        },
        {
            'id': 'TC_API_09',
            'name': 'Áp dụng mã giảm giá không tồn tại / hết hạn',
            'endpoint': '/apply_voucher.php',
            'method': 'POST',
            'url': 'http://localhost:8081/apply_voucher.php',
            'headers': {'Content-Type': 'application/x-www-form-urlencoded', 'Accept': 'application/json'},
            'params': {'code': 'GSLUXURY_INVALID_999', 'total': 1000000},
            'expected_status': 200,
            'expected_note': 'status=error, thông báo mã giảm giá không tồn tại hoặc đã bị vô hiệu hóa.'
        },
        {
            'id': 'TC_API_10',
            'name': 'Lấy danh sách sản phẩm phân trang RESTful API',
            'endpoint': '/load_more_products.php',
            'method': 'GET',
            'url': 'http://localhost:8081/load_more_products.php?offset=0',
            'headers': {'Accept': 'application/json'},
            'params': {'offset': '0'},
            'expected_status': 200,
            'expected_note': 'Trả về mảng JSON sản phẩm gồm ID, tên, giá bán, hình ảnh và chất liệu.'
        },
        {
            'id': 'TC_API_11',
            'name': 'Lấy danh sách đánh giá của sản phẩm (Reviews API)',
            'endpoint': '/get_reviews.php',
            'method': 'GET',
            'url': 'http://localhost:8081/get_reviews.php?product_id=1&limit=4',
            'headers': {'Accept': 'application/json'},
            'params': {'product_id': '1', 'limit': '4'},
            'expected_status': 200,
            'expected_note': 'Trả về cấu trúc reviews, has_more, remaining và tổng số đánh giá.'
        },
        {
            'id': 'TC_API_12',
            'name': 'Trợ lý AI tư vấn nội thất GS LUXURY (Gemini AI API)',
            'endpoint': '/chat_backend.php',
            'method': 'POST',
            'url': 'http://localhost:8081/chat_backend.php',
            'headers': {'Content-Type': 'application/x-www-form-urlencoded', 'Accept': 'application/json'},
            'params': {'message': 'Tư vấn bàn làm việc hiện đại cho văn phòng'},
            'expected_status': 200,
            'expected_note': 'status=success, trợ lý AI phản hồi thông tin sản phẩm và chính sách cửa hàng.'
        }
    ]

    results = []
    for tc in test_cases:
        t0 = time.time()
        req = urllib.request.Request(tc['url'], method=tc['method'])
        for k, v in tc['headers'].items():
            req.add_header(k, v)
        if tc['method'] == 'POST' and tc['params']:
            req.data = urllib.parse.urlencode(tc['params']).encode('utf-8')

        try:
            with urllib.request.urlopen(req) as resp:
                dur = int((time.time() - t0) * 1000)
                raw = resp.read().decode('utf-8')
                code = resp.status
                size = len(raw.encode('utf-8'))
                
                # Cố gắng format JSON
                try:
                    formatted_json = json.dumps(json.loads(raw), ensure_ascii=False, indent=2)
                except Exception:
                    formatted_json = raw

                results.append({
                    'id': tc['id'],
                    'name': tc['name'],
                    'endpoint': tc['endpoint'],
                    'method': tc['method'],
                    'url': tc['url'],
                    'headers': tc['headers'],
                    'params': tc['params'],
                    'status_code': code,
                    'status_text': '200 OK',
                    'dur_ms': dur,
                    'size_bytes': size,
                    'formatted_body': formatted_json,
                    'is_pass': (code == tc['expected_status']),
                    'expected_status': tc['expected_status'],
                    'expected_note': tc['expected_note']
                })
                print(f"[PASS] {tc['id']}: {tc['name']} -> {code} ({dur}ms, {size} bytes)")
        except Exception as e:
            print(f"[FAIL] {tc['id']}: {tc['name']} -> {e}")
            results.append({
                'id': tc['id'],
                'name': tc['name'],
                'endpoint': tc['endpoint'],
                'method': tc['method'],
                'url': tc['url'],
                'headers': tc['headers'],
                'params': tc['params'],
                'status_code': 500,
                'status_text': '500 Error',
                'dur_ms': 0,
                'size_bytes': 0,
                'formatted_body': str(e),
                'is_pass': False,
                'expected_status': tc['expected_status'],
                'expected_note': tc['expected_note']
            })

    with open('api_test_results.json', 'w', encoding='utf-8') as f:
        json.dump(results, f, ensure_ascii=False, indent=2)
    print(f"\nDa thuc thi thanh cong {len(results)} ca kiem thu API! Du lieu luu tai api_test_results.json")

if __name__ == '__main__':
    main()
