/**
 * ***************************************************************************
 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo MES
 * Version: 1.1.1
 *
 * This file is part of Qcadoo.
 *
 * Qcadoo is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation; either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 * ***************************************************************************
 */
package com.qcadoo.mes.workPlans.constants;

import java.util.Set;

import com.google.common.collect.Sets;

public interface WorkPlansConstants {

    String PLUGIN_IDENTIFIER = "workPlans";

    // MODEL
    String MODEL_WORK_PLAN = "workPlan";

    String MODEL_COLUMN_DEFINITION = "columnDefinition";

    String MODEL_COLUMN_FOR_INPUT_PRODUCTS = "columnForInputProducts";

    String MODEL_COLUMN_FOR_OUTPUT_PRODUCTS = "columnForOutputProducts";

    String MODEL_PARAMETER_INPUT_COMPONENT = "parameterInputComponent";

    String MODEL_PARAMETER_OUTPUT_COMPONENT = "parameterOutputComponent";

    String MODEL_OPERATION_INPUT_COMPONENT = "operationInputComponent";

    String MODEL_OPERATION_OUTPUT_COMPONENT = "operationOutputComponent";

    String MODEL_TECHNOLOGY_OPERATION_INPUT_COMPONENT = "technologyOperationInputComponent";

    String MODEL_TECHNOLOGY_OPERATION_OUTPUT_COMPONENT = "technologyOperationOutputComponent";

    String MODEL_ORDER_OPERATION_INPUT_COMPONENT = "orderOperationInputComponent";

    String MODEL_ORDER_OPERATION_OUTPUT_COMPONENT = "orderOperationOutputComponent";

    // VIEW
    String VIEW_WORK_PLAN_DETAILS = "workPlanDetails";

    String VIEW_WORK_PLANS_LIST = "workPlansList";

    Set<String> WORKPLAN_PARAMETERS = Sets.newHashSet("hideDescriptionInWorkPlans", "hideDetailsInWorkPlans",
            "hideTechnologyAndOrderInWorkPlans", "imageUrlInWorkPlan", "dontPrintInputProductsInWorkPlans",
            "dontPrintOutputProductsInWorkPlans");

    Set<String> FILE_EXTENSIONS = Sets.newHashSet("bmp", "gif", "jpg", "jpeg", "png", "tiff", "wmf", "eps");

}
