/**
 * Copyright 2008  Eugene Creswick
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
public interface WizardController {

   /**
    * Register a listener with this wizard.
    * @param listener
    */
   public void addWizardListener(WizardListener listener);
   
   /**
    * Removes the specified listener.
    * @param listener
    */
   public void removeWizardListener(WizardListener listener);

   /**
    * Accessor for the current settings map at any time.
    * 
    * @return The current settings.
    */
   public WizardSettings getSettings();

   /**
    * Gets the path of pages at the current point in the dialog.
    * 
    * @return
    */
   public List<WizardPage> getPath();

   /**
    * Transfer the state of the dialog to the specified page.  If this page
    * has been seen before, move back to that state. Otherwise, add the page to
    * the path and continue from there.
    * 
    * @param page The page to visit.
    */
   public void visitPage(WizardPage page);
   
   /**
    * Forcibly visit the next page.  This is equivalent to the user clicking on
    * "Next >", but this <b>can</b> be done when the button is disabled.
    */
   public void next();
   
   /**
    * Forcibly visit the previous page.  This is equivalent to the user clicking on
    * "< Prev", but this <b>can</b> be done when the button is disabled.
    */
   public void prev();
   
   /**
    * Forcibly finish the wizard.  This is equivalent to the user clicking on
    * "Finish", but this <b>can</b> be done when the button is disabled.
    */
   public void finish();
   
   /**
    * Forcibly cancel the wizard.  This is equivalent to the user clicking on
    * "Cancel", but this <b>can</b> be done when the button is disabled (which
    * should never happen, but if it did..).
    */
   public void cancel();
   
   /**
    * Sets the enabled status of the Next Button.
    * 
    * @param enabled
    */
   public void setNextEnabled(boolean enabled);

   /**
    * Sets the enabled status of the Prev. button.
    * 
    * @param enabled
    */
   public void setPrevEnabled(boolean enabled);

   /**
    * Sets the enabled status of the Finish button.
    * 
    * @param enabled
    */
   public void setFinishEnabled(boolean enabled);


}