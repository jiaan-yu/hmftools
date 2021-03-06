/*
 * This file is generated by jOOQ.
*/
package org.ensembl.database.homo_sapiens_core.tables.records;


import java.sql.Timestamp;

import javax.annotation.Generated;

import org.ensembl.database.homo_sapiens_core.tables.Analysis;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record14;
import org.jooq.Row14;
import org.jooq.impl.UpdatableRecordImpl;
import org.jooq.types.UShort;


/**
 * This class is generated by jOOQ.
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.9.5"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class AnalysisRecord extends UpdatableRecordImpl<AnalysisRecord> implements Record14<UShort, Timestamp, String, String, String, String, String, String, String, String, String, String, String, String> {

    private static final long serialVersionUID = -1802200826;

    /**
     * Setter for <code>homo_sapiens_core_89_37.analysis.analysis_id</code>.
     */
    public void setAnalysisId(UShort value) {
        set(0, value);
    }

    /**
     * Getter for <code>homo_sapiens_core_89_37.analysis.analysis_id</code>.
     */
    public UShort getAnalysisId() {
        return (UShort) get(0);
    }

    /**
     * Setter for <code>homo_sapiens_core_89_37.analysis.created</code>.
     */
    public void setCreated(Timestamp value) {
        set(1, value);
    }

    /**
     * Getter for <code>homo_sapiens_core_89_37.analysis.created</code>.
     */
    public Timestamp getCreated() {
        return (Timestamp) get(1);
    }

    /**
     * Setter for <code>homo_sapiens_core_89_37.analysis.logic_name</code>.
     */
    public void setLogicName(String value) {
        set(2, value);
    }

    /**
     * Getter for <code>homo_sapiens_core_89_37.analysis.logic_name</code>.
     */
    public String getLogicName() {
        return (String) get(2);
    }

    /**
     * Setter for <code>homo_sapiens_core_89_37.analysis.db</code>.
     */
    public void setDb(String value) {
        set(3, value);
    }

    /**
     * Getter for <code>homo_sapiens_core_89_37.analysis.db</code>.
     */
    public String getDb() {
        return (String) get(3);
    }

    /**
     * Setter for <code>homo_sapiens_core_89_37.analysis.db_version</code>.
     */
    public void setDbVersion(String value) {
        set(4, value);
    }

    /**
     * Getter for <code>homo_sapiens_core_89_37.analysis.db_version</code>.
     */
    public String getDbVersion() {
        return (String) get(4);
    }

    /**
     * Setter for <code>homo_sapiens_core_89_37.analysis.db_file</code>.
     */
    public void setDbFile(String value) {
        set(5, value);
    }

    /**
     * Getter for <code>homo_sapiens_core_89_37.analysis.db_file</code>.
     */
    public String getDbFile() {
        return (String) get(5);
    }

    /**
     * Setter for <code>homo_sapiens_core_89_37.analysis.program</code>.
     */
    public void setProgram(String value) {
        set(6, value);
    }

    /**
     * Getter for <code>homo_sapiens_core_89_37.analysis.program</code>.
     */
    public String getProgram() {
        return (String) get(6);
    }

    /**
     * Setter for <code>homo_sapiens_core_89_37.analysis.program_version</code>.
     */
    public void setProgramVersion(String value) {
        set(7, value);
    }

    /**
     * Getter for <code>homo_sapiens_core_89_37.analysis.program_version</code>.
     */
    public String getProgramVersion() {
        return (String) get(7);
    }

    /**
     * Setter for <code>homo_sapiens_core_89_37.analysis.program_file</code>.
     */
    public void setProgramFile(String value) {
        set(8, value);
    }

    /**
     * Getter for <code>homo_sapiens_core_89_37.analysis.program_file</code>.
     */
    public String getProgramFile() {
        return (String) get(8);
    }

    /**
     * Setter for <code>homo_sapiens_core_89_37.analysis.parameters</code>.
     */
    public void setParameters(String value) {
        set(9, value);
    }

    /**
     * Getter for <code>homo_sapiens_core_89_37.analysis.parameters</code>.
     */
    public String getParameters() {
        return (String) get(9);
    }

    /**
     * Setter for <code>homo_sapiens_core_89_37.analysis.module</code>.
     */
    public void setModule(String value) {
        set(10, value);
    }

    /**
     * Getter for <code>homo_sapiens_core_89_37.analysis.module</code>.
     */
    public String getModule() {
        return (String) get(10);
    }

    /**
     * Setter for <code>homo_sapiens_core_89_37.analysis.module_version</code>.
     */
    public void setModuleVersion(String value) {
        set(11, value);
    }

    /**
     * Getter for <code>homo_sapiens_core_89_37.analysis.module_version</code>.
     */
    public String getModuleVersion() {
        return (String) get(11);
    }

    /**
     * Setter for <code>homo_sapiens_core_89_37.analysis.gff_source</code>.
     */
    public void setGffSource(String value) {
        set(12, value);
    }

    /**
     * Getter for <code>homo_sapiens_core_89_37.analysis.gff_source</code>.
     */
    public String getGffSource() {
        return (String) get(12);
    }

    /**
     * Setter for <code>homo_sapiens_core_89_37.analysis.gff_feature</code>.
     */
    public void setGffFeature(String value) {
        set(13, value);
    }

    /**
     * Getter for <code>homo_sapiens_core_89_37.analysis.gff_feature</code>.
     */
    public String getGffFeature() {
        return (String) get(13);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Record1<UShort> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record14 type implementation
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Row14<UShort, Timestamp, String, String, String, String, String, String, String, String, String, String, String, String> fieldsRow() {
        return (Row14) super.fieldsRow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Row14<UShort, Timestamp, String, String, String, String, String, String, String, String, String, String, String, String> valuesRow() {
        return (Row14) super.valuesRow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<UShort> field1() {
        return Analysis.ANALYSIS.ANALYSIS_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Timestamp> field2() {
        return Analysis.ANALYSIS.CREATED;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field3() {
        return Analysis.ANALYSIS.LOGIC_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field4() {
        return Analysis.ANALYSIS.DB;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field5() {
        return Analysis.ANALYSIS.DB_VERSION;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field6() {
        return Analysis.ANALYSIS.DB_FILE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field7() {
        return Analysis.ANALYSIS.PROGRAM;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field8() {
        return Analysis.ANALYSIS.PROGRAM_VERSION;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field9() {
        return Analysis.ANALYSIS.PROGRAM_FILE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field10() {
        return Analysis.ANALYSIS.PARAMETERS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field11() {
        return Analysis.ANALYSIS.MODULE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field12() {
        return Analysis.ANALYSIS.MODULE_VERSION;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field13() {
        return Analysis.ANALYSIS.GFF_SOURCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field14() {
        return Analysis.ANALYSIS.GFF_FEATURE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UShort value1() {
        return getAnalysisId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Timestamp value2() {
        return getCreated();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value3() {
        return getLogicName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value4() {
        return getDb();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value5() {
        return getDbVersion();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value6() {
        return getDbFile();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value7() {
        return getProgram();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value8() {
        return getProgramVersion();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value9() {
        return getProgramFile();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value10() {
        return getParameters();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value11() {
        return getModule();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value12() {
        return getModuleVersion();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value13() {
        return getGffSource();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value14() {
        return getGffFeature();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AnalysisRecord value1(UShort value) {
        setAnalysisId(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AnalysisRecord value2(Timestamp value) {
        setCreated(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AnalysisRecord value3(String value) {
        setLogicName(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AnalysisRecord value4(String value) {
        setDb(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AnalysisRecord value5(String value) {
        setDbVersion(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AnalysisRecord value6(String value) {
        setDbFile(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AnalysisRecord value7(String value) {
        setProgram(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AnalysisRecord value8(String value) {
        setProgramVersion(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AnalysisRecord value9(String value) {
        setProgramFile(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AnalysisRecord value10(String value) {
        setParameters(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AnalysisRecord value11(String value) {
        setModule(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AnalysisRecord value12(String value) {
        setModuleVersion(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AnalysisRecord value13(String value) {
        setGffSource(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AnalysisRecord value14(String value) {
        setGffFeature(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AnalysisRecord values(UShort value1, Timestamp value2, String value3, String value4, String value5, String value6, String value7, String value8, String value9, String value10, String value11, String value12, String value13, String value14) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        value6(value6);
        value7(value7);
        value8(value8);
        value9(value9);
        value10(value10);
        value11(value11);
        value12(value12);
        value13(value13);
        value14(value14);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached AnalysisRecord
     */
    public AnalysisRecord() {
        super(Analysis.ANALYSIS);
    }

    /**
     * Create a detached, initialised AnalysisRecord
     */
    public AnalysisRecord(UShort analysisId, Timestamp created, String logicName, String db, String dbVersion, String dbFile, String program, String programVersion, String programFile, String parameters, String module, String moduleVersion, String gffSource, String gffFeature) {
        super(Analysis.ANALYSIS);

        set(0, analysisId);
        set(1, created);
        set(2, logicName);
        set(3, db);
        set(4, dbVersion);
        set(5, dbFile);
        set(6, program);
        set(7, programVersion);
        set(8, programFile);
        set(9, parameters);
        set(10, module);
        set(11, moduleVersion);
        set(12, gffSource);
        set(13, gffFeature);
    }
}
