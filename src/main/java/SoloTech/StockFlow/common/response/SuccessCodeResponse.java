package SoloTech.StockFlow.common.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SuccessCodeResponse {
    private String responseCode;
    private Object data;
}
