/*
 * Copyright (c) 2020, 2022 by Delphix. All rights reserved.
 */
package sample.masking.algorithm;

import com.delphix.masking.api.plugin.AlgorithmLogicalField;
import com.delphix.masking.api.plugin.MaskingAlgorithm;
import com.delphix.masking.api.plugin.exception.ComponentConfigurationException;
import com.delphix.masking.api.plugin.exception.MaskingException;
import com.delphix.masking.api.plugin.utils.GenericData;
import com.delphix.masking.api.plugin.utils.GenericDataRow;
import com.google.common.collect.ImmutableList;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;

/**
 * This class demonstrates how to write an algorithm that can mask multiple columns at once (aka a
 * multi-column algorithm). Important things to note in this class: - how to define the field names
 * and types (see listFieldsToBeMasked method) - how to extract the field values from the
 * GenericDataRow type - how to write these values back to the GenericDataRow
 *
 * <p>This class expects 2 columns to be defined in its input - startDate and endDate. It shifts
 * startDate by a random number of days from the original startDate, and then shifts endDate to a
 * random number of days from the new masked startDate, to ensure that endDate is always after start
 * date. This manipulation is left intentionally simple to draw the focus to the multi-column
 * process.
 */
public class MultiColumnDateAlgorithm implements MaskingAlgorithm<GenericDataRow> {

    @Override
    public GenericDataRow mask(@Nullable GenericDataRow genericDataRow) throws MaskingException {
        // read the column values from GenericDataRow
        GenericData startDateData = genericDataRow.get("startDate");
        GenericData endDateData = genericDataRow.get("endDate");

        // get the values in the correct type
        LocalDateTime startDate = startDateData.getLocalDateTimeValue();
        LocalDateTime endDate = endDateData.getLocalDateTimeValue();

        Random rand = new Random();

        // mask startDate
        int startDateShiftDays = rand.nextInt(10);
        LocalDateTime startDateMasked = startDate.plusDays(startDateShiftDays);

        // mask endDate. use startDateMasked as a starting point to ensure that endDateMasked is
        // always later
        int endDateShiftDays = rand.nextInt(50);
        LocalDateTime endDateMasked = startDateMasked.plusDays(endDateShiftDays);

        // write the values back to their respective GenericData objects
        startDateData.setValue(startDateMasked);
        endDateData.setValue(endDateMasked);

        // return the genericDataRow, now with updated member objects containing the masked values
        return genericDataRow;
    }

    @Override
    public String getName() {
        return "MultiColumnDateAlgorithm";
    }

    @Override
    public List<AlgorithmLogicalField> listMultiColumnFields() {
        /*
         *  Here we define the column names to be used in the algorithm. These names are only used to reference the
         *  columns within the algorithm and do not need to correspond to the names of the columns on the data source.
         *  For example, our data source may call these 2 fields "dateOfBirth" and "dateOfDeath", however within the
         *  algorithm implementation they will be referenced as "startDate" and "endDate" (see mask method to see how
         *  this is used).
         */
        return ImmutableList.of(
                new AlgorithmLogicalField("startDate", MaskingType.LOCAL_DATE_TIME),
                new AlgorithmLogicalField("endDate", MaskingType.LOCAL_DATE_TIME));
    }

    @Override
    public void validate() throws ComponentConfigurationException {}
}
