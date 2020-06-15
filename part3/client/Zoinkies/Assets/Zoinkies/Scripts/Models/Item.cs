﻿/**
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Models a game item. Items can be anything, from a character type to a spawn location
 * to a reward item.
 */
public class Item {
    public string id { get; set; }
    public int quantity { get; set; }

    public Item() {}

    public Item(string ObjectType, int Quantity) {
        this.id = ObjectType;
        this.quantity = Quantity;
    }
    public override string ToString() {
        return "{Type: " + id + " Quantity: " + quantity + "}";
    }
}