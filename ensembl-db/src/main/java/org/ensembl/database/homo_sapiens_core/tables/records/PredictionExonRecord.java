/*
 * This file is generated by jOOQ.
*/
package org.ensembl.database.homo_sapiens_core.tables.records;


import javax.annotation.Generated;

import org.ensembl.database.homo_sapiens_core.tables.PredictionExon;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record10;
import org.jooq.Row10;
import org.jooq.impl.UpdatableRecordImpl;
import org.jooq.types.UInteger;
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
public class PredictionExonRecord extends UpdatableRecordImpl<PredictionExonRecord> implements Record10<UInteger, UInteger, UShort, UInteger, UInteger, UInteger, Byte, Byte, Double, Double> {

    private static final long serialVersionUID = -1259780987;

    /**
     * Setter for <code>homo_sapiens_core_89_37.prediction_exon.prediction_exon_id</code>.
     */
    public void setPredictionExonId(UInteger value) {
        set(0, value);
    }

    /**
     * Getter for <code>homo_sapiens_core_89_37.prediction_exon.prediction_exon_id</code>.
     */
    public UInteger getPredictionExonId() {
        return (UInteger) get(0);
    }

    /**
     * Setter for <code>homo_sapiens_core_89_37.prediction_exon.prediction_transcript_id</code>.
     */
    public void setPredictionTranscriptId(UInteger value) {
        set(1, value);
    }

    /**
     * Getter for <code>homo_sapiens_core_89_37.prediction_exon.prediction_transcript_id</code>.
     */
    public UInteger getPredictionTranscriptId() {
        return (UInteger) get(1);
    }

    /**
     * Setter for <code>homo_sapiens_core_89_37.prediction_exon.exon_rank</code>.
     */
    public void setExonRank(UShort value) {
        set(2, value);
    }

    /**
     * Getter for <code>homo_sapiens_core_89_37.prediction_exon.exon_rank</code>.
     */
    public UShort getExonRank() {
        return (UShort) get(2);
    }

    /**
     * Setter for <code>homo_sapiens_core_89_37.prediction_exon.seq_region_id</code>.
     */
    public void setSeqRegionId(UInteger value) {
        set(3, value);
    }

    /**
     * Getter for <code>homo_sapiens_core_89_37.prediction_exon.seq_region_id</code>.
     */
    public UInteger getSeqRegionId() {
        return (UInteger) get(3);
    }

    /**
     * Setter for <code>homo_sapiens_core_89_37.prediction_exon.seq_region_start</code>.
     */
    public void setSeqRegionStart(UInteger value) {
        set(4, value);
    }

    /**
     * Getter for <code>homo_sapiens_core_89_37.prediction_exon.seq_region_start</code>.
     */
    public UInteger getSeqRegionStart() {
        return (UInteger) get(4);
    }

    /**
     * Setter for <code>homo_sapiens_core_89_37.prediction_exon.seq_region_end</code>.
     */
    public void setSeqRegionEnd(UInteger value) {
        set(5, value);
    }

    /**
     * Getter for <code>homo_sapiens_core_89_37.prediction_exon.seq_region_end</code>.
     */
    public UInteger getSeqRegionEnd() {
        return (UInteger) get(5);
    }

    /**
     * Setter for <code>homo_sapiens_core_89_37.prediction_exon.seq_region_strand</code>.
     */
    public void setSeqRegionStrand(Byte value) {
        set(6, value);
    }

    /**
     * Getter for <code>homo_sapiens_core_89_37.prediction_exon.seq_region_strand</code>.
     */
    public Byte getSeqRegionStrand() {
        return (Byte) get(6);
    }

    /**
     * Setter for <code>homo_sapiens_core_89_37.prediction_exon.start_phase</code>.
     */
    public void setStartPhase(Byte value) {
        set(7, value);
    }

    /**
     * Getter for <code>homo_sapiens_core_89_37.prediction_exon.start_phase</code>.
     */
    public Byte getStartPhase() {
        return (Byte) get(7);
    }

    /**
     * Setter for <code>homo_sapiens_core_89_37.prediction_exon.score</code>.
     */
    public void setScore(Double value) {
        set(8, value);
    }

    /**
     * Getter for <code>homo_sapiens_core_89_37.prediction_exon.score</code>.
     */
    public Double getScore() {
        return (Double) get(8);
    }

    /**
     * Setter for <code>homo_sapiens_core_89_37.prediction_exon.p_value</code>.
     */
    public void setPValue(Double value) {
        set(9, value);
    }

    /**
     * Getter for <code>homo_sapiens_core_89_37.prediction_exon.p_value</code>.
     */
    public Double getPValue() {
        return (Double) get(9);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Record1<UInteger> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record10 type implementation
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Row10<UInteger, UInteger, UShort, UInteger, UInteger, UInteger, Byte, Byte, Double, Double> fieldsRow() {
        return (Row10) super.fieldsRow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Row10<UInteger, UInteger, UShort, UInteger, UInteger, UInteger, Byte, Byte, Double, Double> valuesRow() {
        return (Row10) super.valuesRow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<UInteger> field1() {
        return PredictionExon.PREDICTION_EXON.PREDICTION_EXON_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<UInteger> field2() {
        return PredictionExon.PREDICTION_EXON.PREDICTION_TRANSCRIPT_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<UShort> field3() {
        return PredictionExon.PREDICTION_EXON.EXON_RANK;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<UInteger> field4() {
        return PredictionExon.PREDICTION_EXON.SEQ_REGION_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<UInteger> field5() {
        return PredictionExon.PREDICTION_EXON.SEQ_REGION_START;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<UInteger> field6() {
        return PredictionExon.PREDICTION_EXON.SEQ_REGION_END;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Byte> field7() {
        return PredictionExon.PREDICTION_EXON.SEQ_REGION_STRAND;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Byte> field8() {
        return PredictionExon.PREDICTION_EXON.START_PHASE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Double> field9() {
        return PredictionExon.PREDICTION_EXON.SCORE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Double> field10() {
        return PredictionExon.PREDICTION_EXON.P_VALUE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UInteger value1() {
        return getPredictionExonId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UInteger value2() {
        return getPredictionTranscriptId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UShort value3() {
        return getExonRank();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UInteger value4() {
        return getSeqRegionId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UInteger value5() {
        return getSeqRegionStart();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UInteger value6() {
        return getSeqRegionEnd();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Byte value7() {
        return getSeqRegionStrand();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Byte value8() {
        return getStartPhase();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Double value9() {
        return getScore();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Double value10() {
        return getPValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PredictionExonRecord value1(UInteger value) {
        setPredictionExonId(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PredictionExonRecord value2(UInteger value) {
        setPredictionTranscriptId(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PredictionExonRecord value3(UShort value) {
        setExonRank(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PredictionExonRecord value4(UInteger value) {
        setSeqRegionId(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PredictionExonRecord value5(UInteger value) {
        setSeqRegionStart(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PredictionExonRecord value6(UInteger value) {
        setSeqRegionEnd(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PredictionExonRecord value7(Byte value) {
        setSeqRegionStrand(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PredictionExonRecord value8(Byte value) {
        setStartPhase(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PredictionExonRecord value9(Double value) {
        setScore(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PredictionExonRecord value10(Double value) {
        setPValue(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PredictionExonRecord values(UInteger value1, UInteger value2, UShort value3, UInteger value4, UInteger value5, UInteger value6, Byte value7, Byte value8, Double value9, Double value10) {
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
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached PredictionExonRecord
     */
    public PredictionExonRecord() {
        super(PredictionExon.PREDICTION_EXON);
    }

    /**
     * Create a detached, initialised PredictionExonRecord
     */
    public PredictionExonRecord(UInteger predictionExonId, UInteger predictionTranscriptId, UShort exonRank, UInteger seqRegionId, UInteger seqRegionStart, UInteger seqRegionEnd, Byte seqRegionStrand, Byte startPhase, Double score, Double pValue) {
        super(PredictionExon.PREDICTION_EXON);

        set(0, predictionExonId);
        set(1, predictionTranscriptId);
        set(2, exonRank);
        set(3, seqRegionId);
        set(4, seqRegionStart);
        set(5, seqRegionEnd);
        set(6, seqRegionStrand);
        set(7, startPhase);
        set(8, score);
        set(9, pValue);
    }
}
