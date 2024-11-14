import org.joda.time.DateTime;
import org.joda.time.Days;
import org.openmrs.module.bahmniemrapi.drugogram.contract.RegimenRow;
import org.openmrs.module.bahmniemrapi.drugogram.contract.TreatmentRegimen;
import org.openmrs.module.bahmniemrapi.drugogram.contract.BaseTableExtension;

import java.util.Date;

public class MonthCalculationExtension extends BaseTableExtension<TreatmentRegimen> {
	@Override
	public void update(TreatmentRegimen treatmentRegimen) {
		Date treatmentStartDate = null;
		try {
			treatmentStartDate = treatmentRegimen.getRows().first().getDate()
		} catch (Exception e) {
			System.out.println("Exception: "+ e.getMessage())
			return;
		}
		for (RegimenRow regimenRow : treatmentRegimen.getRows()) {
			DateTime currentTreatmentDate = new DateTime(regimenRow.getDate());
			Days days = Days.daysBetween(new DateTime(treatmentStartDate), currentTreatmentDate);
			String month = String.format("%.1f", days.getDays()/30.0F);
			regimenRow.setMonth(month);
		}
	}
}