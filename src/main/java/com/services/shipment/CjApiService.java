package com.services.shipment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@Service
public class CjApiService {

    /*
     * application.properties에 적어둔 CJ 개발/운영 서버 주소를 가져온다.
     * 예: https://dxapi-dev.cjlogistics.com:5054
     */
    @Value("${cj.api.base-url}")
    private String baseUrl;

    /*
     * application.properties에 적어둔 고객사 코드를 가져온다.
     * CUST_ID는 CJ대한통운과 계약된 고객사 코드다.
     */
    @Value("${cj.api.cust-id}")
    private String custId;

    /*
     * application.properties에 적어둔 사업자번호를 가져온다.
     * BIZ_REG_NUM은 CJ에 등록된 청구 사업자번호다.
     */
    @Value("${cj.api.biz-reg-num}")
    private String bizRegNum;

    /*
     * 발급받은 토큰번호를 서버 메모리에 임시 저장한다.
     * 이후 주소정제, 운송장 생성, 예약접수 API 호출 때 사용한다.
     */
    private String tokenNum;

    /*
     * 토큰 만료시간을 서버 메모리에 임시 저장한다.
     * 나중에는 이 값을 보고 토큰 만료 여부를 체크하게 된다.
     */
    private String tokenExpireTime;

    /*
     * 유효한 토큰을 가져오는 메서드다.
     *
     * 목적:
     * - 이미 발급받은 토큰이 있으면 기존 토큰을 사용한다.
     * - 토큰이 없으면 CJ 1Day 토큰 발급 API를 호출한다.
     *
     * 사용 이유:
     * - 매번 토큰을 새로 발급받으면 비효율적이다.
     * - CJ 문서상 토큰은 24시간 유효하다.
     * - 토큰 발급 API를 1초에 1회 이상 호출하면 차단될 수 있다.
     */
    public Map<String, Object> getValidToken() {

        /*
         * tokenNum이 null이 아니고 빈 문자열도 아니면
         * 이미 발급받은 토큰이 있다는 뜻이다.
         *
         * 현재는 단순히 토큰 존재 여부만 확인한다.
         * 나중에는 tokenExpireTime과 현재 시간을 비교해서
         * 만료 여부까지 체크해야 한다.
         */
        if (tokenNum != null && !"".equals(tokenNum)) {

            Map<String, Object> result = new HashMap<>();

            result.put("success", true);
            result.put("message", "기존 토큰 사용");
            result.put("tokenNum", tokenNum);
            result.put("tokenExpireTime", tokenExpireTime);

            return result;
        }

        /*
         * 저장된 토큰이 없으면 CJ에 새 토큰을 요청한다.
         */
        return requestOneDayToken();
    }

    /*
     * CJ 1Day 토큰 발급 API를 호출하는 메서드다.
     *
     * 목적:
     * - CUST_ID와 BIZ_REG_NUM을 CJ 서버에 보내서
     *   24시간 사용할 수 있는 TOKEN_NUM을 발급받는다.
     */
    public Map<String, Object> requestOneDayToken() {

        /*
         * CJ 토큰 발급 API 주소를 만든다.
         * baseUrl은 application.properties에서 가져온다.
         */
        String apiUrl = baseUrl + "/ReqOneDayToken";

        /*
         * CJ 문서 기준 토큰 발급 요청 body다.
         *
         * DATA 안에 필수값을 넣어야 한다.
         * - CUST_ID: 고객사 코드
         * - BIZ_REG_NUM: 사업자번호
         *
         * TOKEN_NUM은 여기서 보내는 값이 아니다.
         * TOKEN_NUM은 이 API 성공 후 응답으로 받는 값이다.
         */
        String requestBody = "{\n" +
                "  \"DATA\": {\n" +
                "    \"CUST_ID\": \"" + custId + "\",\n" +
                "    \"BIZ_REG_NUM\": \"" + bizRegNum + "\"\n" +
                "  }\n" +
                "}";

        /*
         * 실제 CJ API 호출은 공통 메서드에서 처리한다.
         *
         * 토큰 발급 API는 문서상 CJ-Gateway-APIKey 생략 가능하므로
         * apiKey에는 null을 넘긴다.
         */
        Map<String, Object> result = callCjApi(apiUrl, requestBody, null);

        /*
         * CJ 응답 body를 문자열로 꺼낸다.
         */
        String responseBody = String.valueOf(result.get("responseBody"));

        /*
         * 응답에 RESULT_CD가 S이면 토큰 발급 성공으로 본다.
         *
         * 성공 응답 예:
         * {
         *   "RESULT_CD": "S",
         *   "RESULT_DETAIL": "Success",
         *   "DATA": {
         *     "TOKEN_NUM": "...",
         *     "TOKEN_EXPRTN_DTM": "..."
         *   }
         * }
         */
        if (responseBody.contains("\"RESULT_CD\":\"S\"")) {

            /*
             * 응답 문자열에서 TOKEN_NUM 값을 꺼낸다.
             * 이 토큰은 이후 다른 CJ API 호출 시 사용한다.
             */
            tokenNum = extractValue(responseBody, "TOKEN_NUM");

            /*
             * 응답 문자열에서 TOKEN_EXPRTN_DTM 값을 꺼낸다.
             * 이 값은 토큰 만료시간이다.
             */
            tokenExpireTime = extractValue(responseBody, "TOKEN_EXPRTN_DTM");

            /*
             * 화면이나 테스트 결과에서 확인할 수 있도록 result에 담는다.
             */
            result.put("tokenNum", tokenNum);
            result.put("tokenExpireTime", tokenExpireTime);
        }

        return result;
    }

    /*
     * CJ API를 실제로 호출하는 공통 메서드다.
     *
     * 목적:
     * - 토큰 발급, 주소정제, 운송장 생성, 예약접수 등
     *   CJ API들은 대부분 POST + JSON 방식이므로
     *   공통 호출 로직을 하나로 묶어둔다.
     *
     * apiUrl:
     * - 호출할 CJ API 주소
     *
     * requestBody:
     * - CJ에 보낼 JSON 문자열
     *
     * apiKey:
     * - CJ-Gateway-APIKey 헤더에 넣을 값
     * - 토큰 발급 API에서는 null
     * - 다른 API에서는 발급받은 tokenNum을 넣는다.
     */
    private Map<String, Object> callCjApi(String apiUrl, String requestBody, String apiKey) {

        Map<String, Object> result = new HashMap<>();
        HttpURLConnection conn = null;

        try {
            /*
             * 문자열 주소를 URL 객체로 변환한다.
             */
            URL url = new URL(apiUrl);

            /*
             * 해당 URL로 HTTP 연결을 준비한다.
             */
            conn = (HttpURLConnection) url.openConnection();

            /*
             * CJ API는 POST 방식으로 호출한다.
             */
            conn.setRequestMethod("POST");

            /*
             * 우리가 보내는 데이터가 JSON 형식임을 알린다.
             */
            conn.setRequestProperty("Content-Type", "application/json");

            /*
             * 응답도 JSON으로 받겠다고 알린다.
             */
            conn.setRequestProperty("Accept", "application/json");

            /*
             * apiKey가 있을 때만 CJ-Gateway-APIKey 헤더를 넣는다.
             *
             * 토큰 발급 API는 apiKey가 null이라 헤더를 생략한다.
             * 주소정제, 운송장 생성, 예약접수 등은 tokenNum을 넣어야 한다.
             */
            if (apiKey != null && !"".equals(apiKey)) {
                conn.setRequestProperty("CJ-Gateway-APIKey", apiKey);
            }

            /*
             * 요청 body를 보낼 수 있도록 설정한다.
             */
            conn.setDoOutput(true);

            /*
             * requestBody 문자열을 CJ 서버로 전송한다.
             */
            try (OutputStream os = conn.getOutputStream()) {
                os.write(requestBody.getBytes("UTF-8"));
                os.flush();
            }

            /*
             * CJ 서버에서 내려준 HTTP 응답 코드를 받는다.
             */
            int responseCode = conn.getResponseCode();

            /*
             * 응답 코드를 보고 정상 응답 스트림 또는 에러 스트림을 선택한다.
             *
             * 400 이상이면 에러 응답이므로 getErrorStream()
             * 400 미만이면 정상 응답이므로 getInputStream()
             */
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(
                            responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream(),
                            "UTF-8"
                    )
            );

            /*
             * CJ 응답을 한 줄씩 읽어서 문자열로 합친다.
             */
            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            br.close();

            /*
             * HTTP 응답 코드가 200번대인지 여부를 success에 담는다.
             *
             * 단, CJ는 HTTP 200이어도 RESULT_CD가 E일 수 있으므로
             * 실제 업무 성공 여부는 RESULT_CD까지 확인해야 한다.
             */
            result.put("success", responseCode >= 200 && responseCode < 300);

            /*
             * HTTP 응답 코드를 결과에 담는다.
             */
            result.put("responseCode", responseCode);

            /*
             * CJ가 내려준 원본 응답 문자열을 결과에 담는다.
             */
            result.put("responseBody", sb.toString());

        } catch (Exception e) {

            /*
             * 통신 중 예외가 발생하면 실패 결과와 에러 메시지를 담는다.
             */
            result.put("success", false);
            result.put("error", e.getMessage());

        } finally {

            /*
             * 연결 객체가 생성되었다면 연결을 종료한다.
             */
            if (conn != null) {
                conn.disconnect();
            }
        }

        return result;
    }

    /*
     * JSON 문자열에서 특정 key의 값을 간단히 꺼내는 메서드다.
     *
     * 예:
     * json = {"TOKEN_NUM":"abc123"}
     * key = "TOKEN_NUM"
     * 결과 = abc123
     *
     * 현재는 연습용 단순 파싱 방식이다.
     * 실무에서는 ObjectMapper 같은 JSON 파서 사용을 권장한다.
     */
    private String extractValue(String json, String key) {

        /*
         * 찾을 패턴을 만든다.
         * 예: "TOKEN_NUM":"
         */
        String findText = "\"" + key + "\":\"";

        /*
         * 해당 key가 시작되는 위치를 찾는다.
         */
        int startIndex = json.indexOf(findText);

        /*
         * key가 없으면 빈 문자열을 반환한다.
         */
        if (startIndex < 0) {
            return "";
        }

        /*
         * 실제 값이 시작되는 위치로 이동한다.
         */
        startIndex = startIndex + findText.length();

        /*
         * 값이 끝나는 큰따옴표 위치를 찾는다.
         */
        int endIndex = json.indexOf("\"", startIndex);

        /*
         * 끝 위치를 못 찾으면 빈 문자열을 반환한다.
         */
        if (endIndex < 0) {
            return "";
        }

        /*
         * 시작 위치부터 끝 위치 전까지 잘라서 값을 반환한다.
         */
        return json.substring(startIndex, endIndex);
    }
}