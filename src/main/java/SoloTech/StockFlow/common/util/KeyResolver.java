package SoloTech.StockFlow.common.util;

public class KeyResolver {
    /**
     * 주어진 템플릿에서 {paramName} 형식을 실제 인자 값으로 치환
     * 예: "stock-{productId}" → "stock-123"
     *
     * @param template     키 템플릿 (예: "stock-{productId}")
     * @param paramNames   메서드 파라미터 이름 배열
     * @param args         메서드 파라미터 값 배열
     * @return 치환된 최종 키 문자열
     */

    public static String resolve(String template, String[] paramNames, Object[] args){
        String resolveKey = template;
        for (int i = 0; i < paramNames.length; i++) {
            String placeholder = "{" + paramNames[i] + "}";
            resolveKey = resolveKey.replace(placeholder, String.valueOf(args[i]));
        }
        return resolveKey;
    }
}
