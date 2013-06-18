/**
 * Copyright 2008 Eugene Creswick
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ciscavate.cjwizard;

import java.util.List;

/**
 * @author rcreswick
 *
 */
public interface PageFactory {

   /**
    * Creates (or retrieves) a wizard page based on the path of pages covered
    * so far between now and the start of the dialog, and the map of settings.
    * 
    * @param path  The list of all WizardPages seen so far.
    * @param settings The Map of settings collected.
    * @return The next WizardPage.
    */
   public WizardPage createPage(List<WizardPage> path, WizardSettings settings);
}
