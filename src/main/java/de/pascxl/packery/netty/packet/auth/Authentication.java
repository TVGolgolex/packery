package de.pascxl.packery.netty.packet.auth;

import java.util.UUID;

/*
 * Copyright 2024 netion contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public record Authentication(String namespace,
                             UUID uniqueId) {

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Authentication target)) {
            return false;
        }
        if (namespace == null && target.namespace == null &&
                uniqueId == null && target.uniqueId == null) {
            return true;
        }
        String firstNameSpace = namespace;
        if (firstNameSpace == null) {
            firstNameSpace = "a";
        }
        String secondNameSpace = target.namespace;
        if (secondNameSpace == null) {
            secondNameSpace = "b";
        }

        UUID firstUniqueId = uniqueId;
        if (firstUniqueId == null) {
            firstUniqueId = UUID.randomUUID();
        }
        UUID secondUniqueId = target.uniqueId;
        if (secondUniqueId == null) {
            secondUniqueId = UUID.randomUUID();
        }
        return firstNameSpace.equalsIgnoreCase(secondNameSpace) &&
                firstUniqueId.toString().equalsIgnoreCase(secondUniqueId.toString());
    }
}