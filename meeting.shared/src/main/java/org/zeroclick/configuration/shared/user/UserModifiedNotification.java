/**
 * Copyright 2017 Djer13

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
limitations under the License.
 */
package org.zeroclick.configuration.shared.user;

import org.zeroclick.meeting.shared.AbstractNotification;

/**
 * @author djer
 *
 */
public class UserModifiedNotification extends AbstractNotification<UserFormData> {

	private static final long serialVersionUID = 2040401559830122856L;

	public UserModifiedNotification(final UserFormData newEvent) {
		super(newEvent);
	}
}
