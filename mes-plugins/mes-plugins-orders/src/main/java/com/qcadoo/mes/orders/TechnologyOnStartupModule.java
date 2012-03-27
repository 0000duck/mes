/**
 * ***************************************************************************
 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo MES
 * Version: 1.1.4
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
package com.qcadoo.mes.orders;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.qcadoo.mes.orders.listeners.TechnologyStateListeners;
import com.qcadoo.mes.technologies.states.TechnologyStateAfterChangeNotifierService;
import com.qcadoo.plugin.api.Module;

@Component
public class TechnologyOnStartupModule extends Module {

    @Autowired
    private TechnologyStateAfterChangeNotifierService technologyStateAfterChangeNotifierService;

    @Autowired
    private TechnologyStateListeners technologyStateListener;

    @Override
    public void enable() {
        registerListeners();
    }

    @Override
    public void enableOnStartup() {
        registerListeners();
    }

    private void registerListeners() {
        technologyStateAfterChangeNotifierService.registerListener(technologyStateListener);
    }

    @Override
    public void disable() {
        technologyStateAfterChangeNotifierService.unregisterListener(technologyStateListener);
    }
}
