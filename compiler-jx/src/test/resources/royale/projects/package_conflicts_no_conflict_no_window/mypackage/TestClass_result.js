/**
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
/**
 * Generated by Apache Royale Compiler from mypackage/TestClass.as
 * mypackage.TestClass
 *
 * @fileoverview
 *
 * @suppress {checkTypes|accessControls}
 */

goog.provide('mypackage.TestClass');



/**
 * @constructor
 */
mypackage.TestClass = function() {

this.event = new Event();
};


/**
 * Prevent renaming of class. Needed for reflection.
 */
goog.exportSymbol('mypackage.TestClass', mypackage.TestClass);


/**
 * @private
 * @type {Event}
 */
mypackage.TestClass.prototype.event = null;


/**
 * Metadata
 *
 * @type {Object.<string, Array.<Object>>}
 */
mypackage.TestClass.prototype.ROYALE_CLASS_INFO = { names: [{ name: 'TestClass', qName: 'mypackage.TestClass', kind: 'class' }] };



/**
 * Reflection
 *
 * @return {Object.<string, Function>}
 */
mypackage.TestClass.prototype.ROYALE_REFLECTION_INFO = function () {
  return {
    methods: function () {
      return {
        'TestClass': { type: '', declaredBy: 'mypackage.TestClass'}
      };
    }
  };
};
/**
 * @const
 * @type {number}
 */
mypackage.TestClass.prototype.ROYALE_REFLECTION_INFO.compileFlags = 9;
