/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.flex.compiler.internal.codegen.js.jx;

import java.util.*;

import org.apache.flex.compiler.codegen.ISubEmitter;
import org.apache.flex.compiler.codegen.js.IJSEmitter;
import org.apache.flex.compiler.common.ASModifier;
import org.apache.flex.compiler.common.ModifiersSet;
import org.apache.flex.compiler.constants.IASKeywordConstants;
import org.apache.flex.compiler.definitions.*;
import org.apache.flex.compiler.definitions.metadata.IMetaTagAttribute;
import org.apache.flex.compiler.internal.codegen.as.ASEmitterTokens;
import org.apache.flex.compiler.internal.codegen.js.JSEmitterTokens;
import org.apache.flex.compiler.internal.codegen.js.JSSessionModel.BindableVarInfo;
import org.apache.flex.compiler.internal.codegen.js.JSSessionModel.ImplicitBindableImplementation;
import org.apache.flex.compiler.internal.codegen.js.JSSubEmitter;
import org.apache.flex.compiler.internal.codegen.js.flexjs.JSFlexJSDocEmitter;
import org.apache.flex.compiler.internal.codegen.js.flexjs.JSFlexJSEmitter;
import org.apache.flex.compiler.internal.codegen.js.flexjs.JSFlexJSEmitterTokens;
import org.apache.flex.compiler.internal.codegen.js.utils.EmitterUtils;
import org.apache.flex.compiler.internal.driver.js.goog.JSGoogConfiguration;
import org.apache.flex.compiler.internal.projects.FlexJSProject;
import org.apache.flex.compiler.internal.tree.as.SetterNode;
import org.apache.flex.compiler.projects.ICompilerProject;
import org.apache.flex.compiler.scopes.IASScope;
import org.apache.flex.compiler.tree.ASTNodeID;
import org.apache.flex.compiler.tree.as.*;
import org.apache.flex.compiler.tree.metadata.IMetaTagNode;
import org.apache.flex.compiler.tree.metadata.IMetaTagsNode;

public class PackageFooterEmitter extends JSSubEmitter implements
        ISubEmitter<IPackageDefinition>
{

    public PackageFooterEmitter(IJSEmitter emitter)
    {
        super(emitter);
    }

    @Override
    public void emit(IPackageDefinition definition)
    {
        IASScope containedScope = definition.getContainedScope();
        ITypeDefinition type = EmitterUtils.findType(containedScope
                .getAllLocalDefinitions());
        if (type == null)
            return;

        getEmitter().emitSourceMapDirective(type.getNode());
    }

    public void emitClassInfo(ITypeNode tnode)
    {
        JSFlexJSDocEmitter doc = (JSFlexJSDocEmitter) getEmitter()
        .getDocEmitter();

		boolean isInterface = tnode instanceof IInterfaceNode;

	    /*
	     * Metadata
	     * 
	     * @type {Object.<string, Array.<Object>>}
	     */
	    writeNewline();
	    writeNewline();
	    writeNewline();
	    doc.begin();
	    writeNewline(" * Metadata");
	    writeNewline(" *");
	    writeNewline(" * @type {Object.<string, Array.<Object>>}");
	    doc.end();

	    // a.B.prototype.AFJS_CLASS_INFO = {  };
	    write(getEmitter().formatQualifiedName(tnode.getQualifiedName()));
	    write(ASEmitterTokens.MEMBER_ACCESS);
	    write(JSEmitterTokens.PROTOTYPE);
	    write(ASEmitterTokens.MEMBER_ACCESS);
	    writeToken(JSFlexJSEmitterTokens.FLEXJS_CLASS_INFO);
	    writeToken(ASEmitterTokens.EQUAL);
	    writeToken(ASEmitterTokens.BLOCK_OPEN);
	
	    // names: [{ name: '', qName: '', kind:'interface|class' }]
	    write(JSFlexJSEmitterTokens.NAMES);
	    writeToken(ASEmitterTokens.COLON);
	    write(ASEmitterTokens.SQUARE_OPEN);
	    writeToken(ASEmitterTokens.BLOCK_OPEN);
	    write(JSFlexJSEmitterTokens.NAME);
	    writeToken(ASEmitterTokens.COLON);
	    write(ASEmitterTokens.SINGLE_QUOTE);
	    write(tnode.getName());
	    write(ASEmitterTokens.SINGLE_QUOTE);
	    writeToken(ASEmitterTokens.COMMA);
	    write(JSFlexJSEmitterTokens.QNAME);
	    writeToken(ASEmitterTokens.COLON);
	    write(ASEmitterTokens.SINGLE_QUOTE);
	    write(getEmitter().formatQualifiedName(tnode.getQualifiedName()));
	    write(ASEmitterTokens.SINGLE_QUOTE);
		writeToken(ASEmitterTokens.COMMA);
		write(JSFlexJSEmitterTokens.FLEXJS_CLASS_INFO_KIND);
		writeToken(ASEmitterTokens.COLON);
		write(ASEmitterTokens.SINGLE_QUOTE);
		if (isInterface) write(JSFlexJSEmitterTokens.FLEXJS_CLASS_INFO_INTERFACE_KIND);
		else write(JSFlexJSEmitterTokens.FLEXJS_CLASS_INFO_CLASS_KIND);
		writeToken(ASEmitterTokens.SINGLE_QUOTE);
	    write(ASEmitterTokens.BLOCK_CLOSE);
	    write(ASEmitterTokens.SQUARE_CLOSE);

	    IExpressionNode[] enodes;
	    if (tnode instanceof IClassNode)
	        enodes = ((IClassNode) tnode).getImplementedInterfaceNodes();
	    else {
			enodes = ((IInterfaceNode) tnode).getExtendedInterfaceNodes();
		}


		boolean needsIEventDispatcher = tnode instanceof IClassNode
				&& ((IClassDefinition) tnode.getDefinition()).needsEventDispatcher(getProject())
				&& getModel().getImplicitBindableImplementation() == ImplicitBindableImplementation.IMPLEMENTS;

		//we can remove the mapping from the model for ImplicitBindableImplementation now
		if (tnode.getDefinition() instanceof IClassDefinition)
				getModel().unregisterImplicitBindableImplementation(
						(IClassDefinition) tnode.getDefinition());

	    if (enodes.length > 0 || needsIEventDispatcher)
	    {
	        writeToken(ASEmitterTokens.COMMA);
	
	        // interfaces: [a.IC, a.ID]
	        write(JSFlexJSEmitterTokens.INTERFACES);
	        writeToken(ASEmitterTokens.COLON);
	        write(ASEmitterTokens.SQUARE_OPEN);
			if (needsIEventDispatcher) {
				//add IEventDispatcher interface to implemented interfaces list
				write(getEmitter().formatQualifiedName(BindableEmitter.DISPATCHER_INTERFACE_QNAME));
				if (enodes.length > 0)
					writeToken(ASEmitterTokens.COMMA);
			}
	        int i = 0;
	        for (IExpressionNode enode : enodes)
	        {
	        	IDefinition edef = enode.resolve(getProject());
	        	if (edef == null)
	        		continue;
	            write(getEmitter().formatQualifiedName(
	                    edef.getQualifiedName()));
	            if (i < enodes.length - 1)
	                writeToken(ASEmitterTokens.COMMA);
	            i++;
	        }
	        write(ASEmitterTokens.SQUARE_CLOSE);
	    }
	    write(ASEmitterTokens.SPACE);
	    write(ASEmitterTokens.BLOCK_CLOSE);
	    write(ASEmitterTokens.SEMICOLON);
	  // Removed this exclusion to support interface reflection (GD)
      //  if (!(tnode instanceof IInterfaceNode))
      //  {
		    writeNewline();
		    writeNewline();
		    writeNewline();
		    doc.begin();
		    writeNewline(" * Prevent renaming of class. Needed for reflection.");
		    doc.end();
		    write(JSFlexJSEmitterTokens.GOOG_EXPORT_SYMBOL);
		    write(ASEmitterTokens.PAREN_OPEN);
		    write(ASEmitterTokens.SINGLE_QUOTE);
		    write(getEmitter().formatQualifiedName(tnode.getQualifiedName()));
		    write(ASEmitterTokens.SINGLE_QUOTE);
		    write(ASEmitterTokens.COMMA);
		    write(ASEmitterTokens.SPACE);
		    write(getEmitter().formatQualifiedName(tnode.getQualifiedName()));
		    write(ASEmitterTokens.PAREN_CLOSE);
		    write(ASEmitterTokens.SEMICOLON);
       // }

	    collectReflectionData(tnode);
	    IMetaTagNode[] metadata = null;
	    IMetaTagsNode metadataTags = tnode.getMetaTags();
	    if (metadataTags != null)
	    	metadata = metadataTags.getAllTags();

		String typeName = getEmitter().formatQualifiedName(tnode.getQualifiedName());

	    emitReflectionData(
	    		typeName,
				reflectionKind,
	    		varData,
	    		accessorData,
	    		methodData,
	    		metadata);
	    
	    emitExportProperties(typeName, exportProperties, exportSymbols);
    }

    public enum ReflectionKind{
		CLASS,
		INTERFACE
	}
    
    public class VariableData
    {
    	public String name;
    	public String type;
		public Boolean isStatic = false;
    	public IMetaTagNode[] metaData;
    }
    
    public class MethodData
    {
    	public String name;
    	public String type;
		public Boolean isStatic = false;
    	public String declaredBy;
		public IParameterNode [] parameters;
    	public IMetaTagNode[] metaData;
    }

	public class AccessorData extends MethodData
	{
		public String access;
	}

	private ArrayList<VariableData> varData;
    private ArrayList<AccessorData> accessorData;
    private ArrayList<MethodData> methodData;
	private ReflectionKind reflectionKind;
    private ArrayList<String> exportProperties;
    private ArrayList<String> exportSymbols;
    
    public void collectReflectionData(ITypeNode tnode)
    {
    	JSFlexJSEmitter fjs = (JSFlexJSEmitter)getEmitter();
    	exportProperties = new ArrayList<String>();
    	exportSymbols = new ArrayList<String>();
		ICompilerProject project = getWalker().getProject();
    	Set<String> exportMetadata = Collections.<String> emptySet();
    	if (project instanceof FlexJSProject)
    	{
    		FlexJSProject fjsp = ((FlexJSProject)project);
    		if (fjsp.config != null)
    			exportMetadata = fjsp.config.getCompilerKeepCodeWithMetadata();
    	}
    	varData = new ArrayList<VariableData>();
    	accessorData = new ArrayList<AccessorData>();
    	methodData = new ArrayList<MethodData>();
    	/*
	     * Reflection
	     * 
	     * @return {Object.<string, Function>}
	     */
        IDefinitionNode[] dnodes;
		String name;
		//bindables:
		HashMap<String, BindableVarInfo> bindableVars = getModel().getBindableVars();
        boolean isInterface = tnode instanceof IInterfaceNode;
	    if (!isInterface)
	        dnodes = ((IClassNode) tnode).getAllMemberNodes();
	    else
	        dnodes = ((IInterfaceNode) tnode).getAllMemberDefinitionNodes();
		reflectionKind = isInterface ? ReflectionKind.INTERFACE : ReflectionKind.CLASS;

        for (IDefinitionNode dnode : dnodes)
        {
            ModifiersSet modifierSet = dnode.getDefinition().getModifiers();
            boolean isStatic = (modifierSet != null && modifierSet
                    .hasModifier(ASModifier.STATIC));
            if ((dnode.getNodeID() == ASTNodeID.VariableID ||
            		dnode.getNodeID() == ASTNodeID.BindableVariableID))
            {
            	IVariableNode varNode = (IVariableNode)dnode;
                String ns = varNode.getNamespace();
				boolean isConst = varNode.isConst();
				if (isConst) {
					//todo consider outputting consts, none output for now
					continue;
				}
                if (ns == IASKeywordConstants.PUBLIC || isInterface)
                {
                	name = varNode.getName();

					IMetaTagsNode metaData = varNode.getMetaTags();
					//first deal with 'Bindable' upgrades to getters/setters
					if (!isInterface && bindableVars.containsKey(name)
							&& bindableVars.get(name).namespace == IASKeywordConstants.PUBLIC) {

						AccessorData bindableAccessor = new AccessorData();
						bindableAccessor.name = name;
						bindableAccessor.access = "readwrite";
						bindableAccessor.type = bindableVars.get(name).type;
						bindableAccessor.declaredBy = fjs.formatQualifiedName(tnode.getQualifiedName(), true);
						bindableAccessor.isStatic = isStatic;
						//attribute the metadata from the var definition to the Bindable Accessor implementation
						if (metaData != null)
						{
							IMetaTagNode[] tags = metaData.getAllTags();
							if (tags.length > 0)
								bindableAccessor.metaData = tags;
						}
						accessorData.add(bindableAccessor);
						//skip processing this varNode as a variable, it has now be added as an accessor
						continue;
					}


                	VariableData data = new VariableData();
                	varData.add(data);
                	data.name = name;
					data.isStatic = isStatic;
					String qualifiedTypeName =	varNode.getVariableTypeNode().resolveType(getProject()).getQualifiedName();
					data.type = fjs.formatQualifiedName(qualifiedTypeName, true);

            	    if (metaData != null)
            	    {
            	    	IMetaTagNode[] tags = metaData.getAllTags();
            	    	if (tags.length > 0)
            	    	{
            	    		data.metaData = tags;
            	    		for (IMetaTagNode tag : tags)
            	    		{
            	    			String tagName =  tag.getTagName();
            	    			if (exportMetadata.contains(tagName))
            	    			{
            	    				if (data.isStatic)
            	    					exportSymbols.add(data.name);
            	    				else
                	    				exportProperties.add(data.name);
            	    			}
            	    		}
            	    	}
            	    }
                }
            }
        }

        if (getModel().hasStaticBindableVars()) {
			//we have an implicit implementation of a static event dispatcher
			//so add the 'staticEventDispatcher' accessor to the reflection data
			AccessorData staticEventDispatcher = new AccessorData();
			staticEventDispatcher.name = BindableEmitter.STATIC_DISPATCHER_GETTER;
			staticEventDispatcher.access = "readonly";
			staticEventDispatcher.type = fjs.formatQualifiedName(BindableEmitter.DISPATCHER_CLASS_QNAME, true);
			staticEventDispatcher.declaredBy = fjs.formatQualifiedName(tnode.getQualifiedName(), true);
			staticEventDispatcher.isStatic = true;
			accessorData.add(staticEventDispatcher);
		}
        
	    HashMap<String, AccessorData> accessorMap = new HashMap<String, AccessorData>();
        for (IDefinitionNode dnode : dnodes)
        {
            ModifiersSet modifierSet = dnode.getDefinition().getModifiers();
            boolean isStatic = (modifierSet != null && modifierSet
                    .hasModifier(ASModifier.STATIC));
            if ((dnode.getNodeID() == ASTNodeID.GetterID ||
            		dnode.getNodeID() == ASTNodeID.SetterID))
            {
            	IFunctionNode fnNode = (IFunctionNode)dnode;
                String ns = fnNode.getNamespace();
                if (ns == IASKeywordConstants.PUBLIC || isInterface)
                {
					String accessorName = fnNode.getName();
                	AccessorData data = accessorMap.get(accessorName);
					if (data == null) {
						data = new AccessorData();
					}
                	data.name = fnNode.getName();

                	if (!accessorData.contains(data)) accessorData.add(data);
            	    if (dnode.getNodeID() == ASTNodeID.GetterID) {
						data.type = fnNode.getReturnTypeNode().resolveType(getProject()).getQualifiedName();
						if (data.access == null) {
							data.access = "readonly";
						} else data.access = "readwrite";
					}
            	    else {
						data.type = ((SetterNode)fnNode).getVariableTypeNode().resolveType(getProject()).getQualifiedName();
						if (data.access == null) {
							data.access = "writeonly";
						} else data.access = "readwrite";
					}
                	accessorMap.put(data.name, data);
            	    data.type = fjs.formatQualifiedName(data.type, true);
            	    IClassNode declarer = (IClassNode)fnNode.getAncestorOfType(IClassNode.class);
            	    String declarant = fjs.formatQualifiedName(tnode.getQualifiedName(), true);
            	    if (declarer != null)
            	    	declarant = fjs.formatQualifiedName(declarer.getQualifiedName(), true);
            	    data.declaredBy = declarant;
					data.isStatic = isStatic;
            	    IMetaTagsNode metaData = fnNode.getMetaTags();
            	    if (metaData != null)
            	    {
            	    	IMetaTagNode[] tags = metaData.getAllTags();
            	    	if (tags.length > 0)
            	    	{
            	    		data.metaData = tags;
        	    			/* accessors don't need exportProp since they are referenced via the defineProp data structure
            	    		for (IMetaTagNode tag : tags)
            	    		{
            	    			String tagName =  tag.getTagName();
            	    			if (exportMetadata.contains(tagName))
            	    			{
            	    				if (data.isStatic)
            	    					exportSymbols.add(data.name);
            	    				else
                	    				exportProperties.add(data.name);
            	    			}
            	    		}
            	    		*/
            	    	}
            	    }
                }
            }
        }
        for (IDefinitionNode dnode : dnodes)
        {
            ModifiersSet modifierSet = dnode.getDefinition().getModifiers();
            boolean isStatic = (modifierSet != null && modifierSet
                    .hasModifier(ASModifier.STATIC));
            if (dnode.getNodeID() == ASTNodeID.FunctionID )
            {
            	IFunctionNode fnNode = (IFunctionNode)dnode;
                String ns = fnNode.getNamespace();
                if (ns == IASKeywordConstants.PUBLIC || isInterface)
                {
                	MethodData data = new MethodData();
					data.isStatic = isStatic;
                	methodData.add(data);
                	data.name = fnNode.getName();
					String qualifiedTypeName =	fnNode.getReturnType();
					if (!(qualifiedTypeName.equals("") || qualifiedTypeName.equals("void"))) {
							qualifiedTypeName = fnNode.getReturnTypeNode().resolveType(getProject()).getQualifiedName();
					}
					data.type = fjs.formatQualifiedName(qualifiedTypeName, true);
            	    ITypeNode declarer;
            	    if (isInterface)
            	    	declarer = (IInterfaceNode)fnNode.getAncestorOfType(IInterfaceNode.class);
            	    else
            	    	declarer = (IClassNode)fnNode.getAncestorOfType(IClassNode.class);
            	    String declarant = fjs.formatQualifiedName(tnode.getQualifiedName(), true);
            	    if (declarer != null)
            	    	declarant = fjs.formatQualifiedName(declarer.getQualifiedName(), true);
            	    data.declaredBy = declarant;
            	    IMetaTagsNode metaData = fnNode.getMetaTags();
            	    if (metaData != null)
            	    {
            	    	IMetaTagNode[] tags = metaData.getAllTags();
            	    	if (tags.length > 0)
            	    	{
            	    		data.metaData = tags;
            	    		for (IMetaTagNode tag : tags)
            	    		{
            	    			String tagName =  tag.getTagName();
            	    			if (exportMetadata.contains(tagName))
            	    			{
            	    				if (data.isStatic)
            	    					exportSymbols.add(data.name);
            	    				else
                	    				exportProperties.add(data.name);
            	    			}
            	    		}
            	    	}
            	    }
					IParameterNode[] paramNodes = fnNode.getParameterNodes();
					if (paramNodes != null) {
						data.parameters = paramNodes;
					}
				}
            }
        }
    }




    private void emitReflectionDataStart(String typeName) {
		JSFlexJSDocEmitter doc = (JSFlexJSDocEmitter) getEmitter()
				.getDocEmitter();
	    /*
	     * Reflection
	     *
	     * @return {Object.<string, Function>}
	     */

		writeNewline();
		writeNewline();
		writeNewline();
		writeNewline();
		doc.begin();
		writeNewline(" * Reflection");
		writeNewline(" *");
		writeNewline(" * @return {Object.<string, Function>}");
		doc.end();

		// a.B.prototype.FLEXJS_REFLECTION_INFO = function() {
		write(typeName);
		write(ASEmitterTokens.MEMBER_ACCESS);
		write(JSEmitterTokens.PROTOTYPE);
		write(ASEmitterTokens.MEMBER_ACCESS);
		writeToken(JSFlexJSEmitterTokens.FLEXJS_REFLECTION_INFO);
		writeToken(ASEmitterTokens.EQUAL);
		writeToken(ASEmitterTokens.FUNCTION);
		write(ASEmitterTokens.PAREN_OPEN);
		writeToken(ASEmitterTokens.PAREN_CLOSE);
		write(ASEmitterTokens.BLOCK_OPEN);

		indentPush();
		writeNewline();
		// return {
		writeToken(ASEmitterTokens.RETURN);
		write(ASEmitterTokens.BLOCK_OPEN);
		indentPush();
		writeNewline();
	}

	private void emitReflectionDataEnd(String typeName) {
		writeNewline();
		// close return object
		write(ASEmitterTokens.BLOCK_CLOSE);
		write(ASEmitterTokens.SEMICOLON);

		// close function
		indentPop();
		writeNewline();
		write(ASEmitterTokens.BLOCK_CLOSE);
		writeNewline(ASEmitterTokens.SEMICOLON);
	}
    
    public void emitReflectionData(
			String typeName,
			ReflectionKind outputType,
    		List<VariableData> varData,
    		List<AccessorData> accessorData,
    		List<MethodData> methodData,
    		IMetaTagNode[] metaData

    		)
    {

		emitReflectionDataStart(typeName);
		int count;
		if (outputType == ReflectionKind.CLASS) {
			// variables: function() {
			write("variables");
			writeToken(ASEmitterTokens.COLON);
			writeToken(ASEmitterTokens.FUNCTION);
			write(ASEmitterTokens.PAREN_OPEN);
			writeToken(ASEmitterTokens.PAREN_CLOSE);
			write(ASEmitterTokens.BLOCK_OPEN);
			if (varData.size() == 0) {
				//return {};},
				writeEmptyContent(true, true);
			} else {


				indentPush();
				writeNewline();
				// return {
				writeToken(ASEmitterTokens.RETURN);
				write(ASEmitterTokens.BLOCK_OPEN);
				indentPush();

				count = 0;
				for (VariableData var : varData) {
					if (count > 0)
						write(ASEmitterTokens.COMMA);
					writeNewline();
					count++;
					// varname: { type: typename
					write(ASEmitterTokens.SINGLE_QUOTE);
					write(var.name);
					write(ASEmitterTokens.SINGLE_QUOTE);
					writeToken(ASEmitterTokens.COLON);
					writeToken(ASEmitterTokens.BLOCK_OPEN);
					write("type");
					writeToken(ASEmitterTokens.COLON);
					write(ASEmitterTokens.SINGLE_QUOTE);
					write(var.type);
					write(ASEmitterTokens.SINGLE_QUOTE);
					if (var.isStatic) {
						writeIsStatic();
					}
					IMetaTagNode[] tags = var.metaData;
					if (tags != null) {
						writeToken(ASEmitterTokens.COMMA);
						writeMetaData(tags);
					}
					// close object
					write(ASEmitterTokens.BLOCK_CLOSE);
				}
				indentPop();
				writeNewline();
				write(ASEmitterTokens.BLOCK_CLOSE);
				write(ASEmitterTokens.SEMICOLON);
				indentPop();
				writeNewline();
				// close variable function
				write(ASEmitterTokens.BLOCK_CLOSE);
				write(ASEmitterTokens.COMMA);
				writeNewline();
			}
		}
	    
	    // accessors: function() {
	    write("accessors");
	    writeToken(ASEmitterTokens.COLON);
	    writeToken(ASEmitterTokens.FUNCTION);
	    write(ASEmitterTokens.PAREN_OPEN);
	    writeToken(ASEmitterTokens.PAREN_CLOSE);
	    write(ASEmitterTokens.BLOCK_OPEN);

		if (accessorData.size() == 0) {
			//return {};},
			writeEmptyContent(true, true);
		} else {
			indentPush();
			writeNewline();
			// return {
			writeToken(ASEmitterTokens.RETURN);
			write(ASEmitterTokens.BLOCK_OPEN);
			indentPush();

			count = 0;
			for (AccessorData accessor : accessorData)
			{
				if (count > 0)
					write(ASEmitterTokens.COMMA);
				writeNewline();
				count++;
				// accessorname: { type: typename
				write(ASEmitterTokens.SINGLE_QUOTE);
				write(accessor.name);
				write(ASEmitterTokens.SINGLE_QUOTE);
				writeToken(ASEmitterTokens.COLON);
				writeToken(ASEmitterTokens.BLOCK_OPEN);
				write("type");
				writeToken(ASEmitterTokens.COLON);
				write(ASEmitterTokens.SINGLE_QUOTE);
				write(accessor.type);
				write(ASEmitterTokens.SINGLE_QUOTE);
				if (accessor.isStatic) {
					writeIsStatic();
				}
				writeToken(ASEmitterTokens.COMMA);
				write("access");
				writeToken(ASEmitterTokens.COLON);
				write(ASEmitterTokens.SINGLE_QUOTE);
				write(accessor.access);
				write(ASEmitterTokens.SINGLE_QUOTE);
				writeToken(ASEmitterTokens.COMMA);
				write("declaredBy");
				writeToken(ASEmitterTokens.COLON);
				write(ASEmitterTokens.SINGLE_QUOTE);
				write(accessor.declaredBy);
				write(ASEmitterTokens.SINGLE_QUOTE);
				IMetaTagNode[] tags = accessor.metaData;
				if (tags != null)
				{
					writeToken(ASEmitterTokens.COMMA);
					writeMetaData(tags);
				}
				// close object
				write(ASEmitterTokens.BLOCK_CLOSE);
			}
			indentPop();
			writeNewline();
			write(ASEmitterTokens.BLOCK_CLOSE);
			write(ASEmitterTokens.SEMICOLON);
			indentPop();
			writeNewline();
			// close accessor function
			write(ASEmitterTokens.BLOCK_CLOSE);
			write(ASEmitterTokens.COMMA);
			writeNewline();

		}

	    // methods: function() {
	    write("methods");
	    writeToken(ASEmitterTokens.COLON);
	    writeToken(ASEmitterTokens.FUNCTION);
	    write(ASEmitterTokens.PAREN_OPEN);
	    writeToken(ASEmitterTokens.PAREN_CLOSE);
	    write(ASEmitterTokens.BLOCK_OPEN);
		if (methodData.size() == 0) {
			//return {};},
			writeEmptyContent(false, false);
		} else {
			indentPush();
			writeNewline();
			// return {
			writeToken(ASEmitterTokens.RETURN);
			write(ASEmitterTokens.BLOCK_OPEN);
			indentPush();

			count = 0;
			for (MethodData method : methodData)
			{
				if (count > 0)
					write(ASEmitterTokens.COMMA);
				writeNewline();
				count++;
				// methodname: { type: typename
				write(ASEmitterTokens.SINGLE_QUOTE);
				write(method.name);
				write(ASEmitterTokens.SINGLE_QUOTE);
				writeToken(ASEmitterTokens.COLON);
				writeToken(ASEmitterTokens.BLOCK_OPEN);
				write("type");
				writeToken(ASEmitterTokens.COLON);
				write(ASEmitterTokens.SINGLE_QUOTE);
				write(method.type);
				write(ASEmitterTokens.SINGLE_QUOTE);
				if (method.isStatic) {
					writeIsStatic();
				}
				writeToken(ASEmitterTokens.COMMA);
				write("declaredBy");
				writeToken(ASEmitterTokens.COLON);
				write(ASEmitterTokens.SINGLE_QUOTE);
				write(method.declaredBy);
				write(ASEmitterTokens.SINGLE_QUOTE);

				IParameterNode[] params = method.parameters;
				//only output params if there are any
				if (params!=null && params.length > 0) {
					writeToken(ASEmitterTokens.COMMA);
					writeParameters(params);
				}
				IMetaTagNode[] metas = method.metaData;
				if (metas != null)
				{
					writeToken(ASEmitterTokens.COMMA);
					writeMetaData(metas);
				}

				// close object
				write(ASEmitterTokens.BLOCK_CLOSE);
			}
			// close return
			indentPop();
			writeNewline();
			write(ASEmitterTokens.BLOCK_CLOSE);
			write(ASEmitterTokens.SEMICOLON);
			indentPop();
			writeNewline();
			// close method function
			write(ASEmitterTokens.BLOCK_CLOSE);
		}



    	if (metaData != null && metaData.length > 0)
    	{
    		write(ASEmitterTokens.COMMA);
    	    writeNewline();
    	    writeMetaData(metaData);
    	}            	    	
	    
	    indentPop();

		emitReflectionDataEnd(typeName);
    }

    private void writeStringArray(ArrayList<String> sequence) {
    	int l = sequence.size();
		int count = 0;
    	writeToken(ASEmitterTokens.SQUARE_OPEN);
		for (String item :sequence) {
			if (count>0) {
				write(ASEmitterTokens.COMMA);
				write(ASEmitterTokens.SPACE);
			}
			write(ASEmitterTokens.SINGLE_QUOTE);
			write(item);
			write(ASEmitterTokens.SINGLE_QUOTE);
			count++;
		}
		if (l>0) write(ASEmitterTokens.SPACE);
		writeToken(ASEmitterTokens.SQUARE_CLOSE);
	}

	private void writeIsStatic() {
		writeToken(ASEmitterTokens.COMMA);
		write("isStatic");
		writeToken(ASEmitterTokens.COLON);
		writeToken(ASEmitterTokens.TRUE);
	}

	private void writeEmptyContent(Boolean appendComma, Boolean includeNewline) {
		//return {};
		writeToken(ASEmitterTokens.RETURN);
		write(ASEmitterTokens.BLOCK_OPEN);
		write(ASEmitterTokens.BLOCK_CLOSE);
		write(ASEmitterTokens.SEMICOLON);
		// close empty content function
		write(ASEmitterTokens.BLOCK_CLOSE);
		if (appendComma) write(ASEmitterTokens.COMMA);
		if (includeNewline) writeNewline();
	}

	private void writeParameters(IParameterNode[] params)
	{
		// parameters: function() {
		write("parameters");
		writeToken(ASEmitterTokens.COLON);
		writeToken(ASEmitterTokens.FUNCTION);
		write(ASEmitterTokens.PAREN_OPEN);
		writeToken(ASEmitterTokens.PAREN_CLOSE);
		writeToken(ASEmitterTokens.BLOCK_OPEN);
		// return [ array of parameter definitions ]
		writeToken(ASEmitterTokens.RETURN);
		writeToken(ASEmitterTokens.SQUARE_OPEN);
		write(ASEmitterTokens.SPACE);

		int len = params.length;
		for (int i = 0; i < len ; i++) {
			IParameterDefinition parameterDefinition = (IParameterDefinition) params[i].getDefinition();
			writeToken(ASEmitterTokens.BLOCK_OPEN);
			write("index");
			writeToken(ASEmitterTokens.COLON);
			write(Integer.toString(i+1));
			write(ASEmitterTokens.COMMA);
			write(ASEmitterTokens.SPACE);
			write("type");
			writeToken(ASEmitterTokens.COLON);
			write(ASEmitterTokens.SINGLE_QUOTE);
			write(parameterDefinition.resolveType(getProject()).getQualifiedName());
			write(ASEmitterTokens.SINGLE_QUOTE);

			write(ASEmitterTokens.COMMA);
			write(ASEmitterTokens.SPACE);
			write("optional");
			writeToken(ASEmitterTokens.COLON);
			writeToken(parameterDefinition.hasDefaultValue() ? ASEmitterTokens.TRUE :  ASEmitterTokens.FALSE);

			write(ASEmitterTokens.BLOCK_CLOSE);
			if (i < len-1) write(ASEmitterTokens.COMMA);
		}

		// close array of parameter definitions
		write(ASEmitterTokens.SPACE);
		write(ASEmitterTokens.SQUARE_CLOSE);
		writeToken(ASEmitterTokens.SEMICOLON);
		// close function
		write(ASEmitterTokens.BLOCK_CLOSE);
	}
    
    private void writeMetaData(IMetaTagNode[] tags)
    {
    	JSGoogConfiguration config = ((FlexJSProject)getWalker().getProject()).config;
    	Set<String> allowedNames = config.getCompilerKeepAs3Metadata();
    	
	    // metadata: function() {
		write("metadata");
	    writeToken(ASEmitterTokens.COLON);
	    writeToken(ASEmitterTokens.FUNCTION);
	    write(ASEmitterTokens.PAREN_OPEN);
	    writeToken(ASEmitterTokens.PAREN_CLOSE);
	    writeToken(ASEmitterTokens.BLOCK_OPEN);
	    // return [ array of metadata tags ]
	    writeToken(ASEmitterTokens.RETURN);
	    writeToken(ASEmitterTokens.SQUARE_OPEN);

		ArrayList<IMetaTagNode> filteredTags = new ArrayList<IMetaTagNode>(tags.length);
		for (IMetaTagNode tag : tags)
		{
			if (allowedNames.contains(tag.getTagName())) filteredTags.add(tag);

		}

	    int count = 0;
		int len = filteredTags.size();
	    for (IMetaTagNode tag : filteredTags)
	    {


	    	count++;
    	    // { name: <tag name>
    	    writeToken(ASEmitterTokens.BLOCK_OPEN);
    	    write("name");
    	    writeToken(ASEmitterTokens.COLON);
    	    write(ASEmitterTokens.SINGLE_QUOTE);
    	    write(tag.getTagName());
    	    write(ASEmitterTokens.SINGLE_QUOTE);
    	    IMetaTagAttribute[] args = tag.getAllAttributes();
    	    if (args.length > 0)
    	    {
        		writeToken(ASEmitterTokens.COMMA);
        	    
        	    // args: [
        	    write("args");
        	    writeToken(ASEmitterTokens.COLON);
        	    writeToken(ASEmitterTokens.SQUARE_OPEN);
        	    
        	    for (int j = 0; j < args.length; j++)
        	    {
        	    	if (j > 0)
        	    	{
                		writeToken(ASEmitterTokens.COMMA);
        	    	}
        	    	// { key: key, value: value }
        	    	IMetaTagAttribute arg = args[j];
            	    writeToken(ASEmitterTokens.BLOCK_OPEN);
            	    write("key");
            	    writeToken(ASEmitterTokens.COLON);
            	    write(ASEmitterTokens.SINGLE_QUOTE);
            	    String key = arg.getKey();
            	    write(key == null ? "" : key);
            	    write(ASEmitterTokens.SINGLE_QUOTE);
            		writeToken(ASEmitterTokens.COMMA);
            	    write("value");
            	    writeToken(ASEmitterTokens.COLON);
            	    write(ASEmitterTokens.SINGLE_QUOTE);
            	    write(formatJSStringValue(arg.getValue()));
            	    write(ASEmitterTokens.SINGLE_QUOTE);
					write(ASEmitterTokens.SPACE);
            	    write(ASEmitterTokens.BLOCK_CLOSE);
        	    }
        	    // close array of args
				write(ASEmitterTokens.SPACE);
        	    write(ASEmitterTokens.SQUARE_CLOSE);
    	    }
    	    // close metadata object
			write(ASEmitterTokens.SPACE);
    	    write(ASEmitterTokens.BLOCK_CLOSE);
			if (count > 0 && count < len)
			{
				writeToken(ASEmitterTokens.COMMA);
			}
	    }
	    // close array of metadatas
		write(ASEmitterTokens.SPACE);
	    write(ASEmitterTokens.SQUARE_CLOSE);
	    writeToken(ASEmitterTokens.SEMICOLON);
	    // close function
	    write(ASEmitterTokens.BLOCK_CLOSE);
    }

    private String formatJSStringValue(String value) {
		//todo: check other possible metadata values for any need for js string escaping etc
    	value = value.replace("'","\\'");
    	return value;
	}
    
    public void emitExportProperties(String typeName, ArrayList<String> exportProperties, ArrayList<String> exportSymbols)
    {
    	for (String prop : exportSymbols)
    	{
    		write(JSFlexJSEmitterTokens.GOOG_EXPORT_SYMBOL);
    		write(ASEmitterTokens.PAREN_OPEN);
    		write(ASEmitterTokens.SINGLE_QUOTE);
    		write(typeName);
    		write(ASEmitterTokens.MEMBER_ACCESS);
    		write(prop);
    		write(ASEmitterTokens.SINGLE_QUOTE);
    		write(ASEmitterTokens.COMMA);
    		write(ASEmitterTokens.SPACE);
    		write(typeName);
    		write(ASEmitterTokens.MEMBER_ACCESS);
    		write(prop);
    		write(ASEmitterTokens.PAREN_CLOSE);
    		writeNewline(ASEmitterTokens.SEMICOLON);
    	}
    	for (String prop : exportProperties)
    	{
    		write(JSFlexJSEmitterTokens.GOOG_EXPORT_PROPERTY);
    		write(ASEmitterTokens.PAREN_OPEN);
    		write(typeName);
    		write(ASEmitterTokens.MEMBER_ACCESS);
    		write(JSEmitterTokens.PROTOTYPE);
    		write(ASEmitterTokens.COMMA);
    		write(ASEmitterTokens.SPACE);
    		write(ASEmitterTokens.SINGLE_QUOTE);
    		write(prop);
    		write(ASEmitterTokens.SINGLE_QUOTE);
    		write(ASEmitterTokens.COMMA);
    		write(ASEmitterTokens.SPACE);
    		write(typeName);
    		write(ASEmitterTokens.MEMBER_ACCESS);
    		write(JSEmitterTokens.PROTOTYPE);
    		write(ASEmitterTokens.MEMBER_ACCESS);
    		write(prop);
    		write(ASEmitterTokens.PAREN_CLOSE);
    		writeNewline(ASEmitterTokens.SEMICOLON);
    	}
    }
}
