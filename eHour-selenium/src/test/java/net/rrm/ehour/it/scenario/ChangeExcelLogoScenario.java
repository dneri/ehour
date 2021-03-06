package net.rrm.ehour.it.scenario;

import net.rrm.ehour.it.AbstractScenario;
import net.rrm.ehour.it.EhourApplicationDriver;
import net.rrm.ehour.it.WicketBy;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class ChangeExcelLogoScenario extends AbstractScenario {
    @Test
    public void should_upload_excel() {
        EhourApplicationDriver.loginAdmin();

        Driver.findElement(WicketBy.wicketPath("configTabs_tabs-container_tabs_3_link_title")).click();

        assertTrue(Driver.findElement(WicketBy.wicketPath("configTabs_panel_border_greySquaredFrame_border__body_form_excelPreview")).getText().matches("^[\\s\\S]*Preview in excel[\\s\\S]*$"));
    }
}
