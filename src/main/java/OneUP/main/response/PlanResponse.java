package OneUP.main.response;

import java.time.LocalDate;

public record PlanResponse(
        String nameCreator,
        String name,
        String rowData,
        LocalDate dateCreate
) {
}
