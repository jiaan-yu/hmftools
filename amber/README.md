# AMBER
AMBER is designed to generate a tumor BAF file for use in PURPLE. 

Looking directly at the bam files, it locates heterozygous sites within the reference sample then calculates the allelic frequency of corresponding sites in the tumour. Finally, the Bioconductor copy number package is used to generate pcf segments from the BAF file.

Prior versions of AMBER relied on mpileups of the tumor and reference rather than the bams themselves. This method is deprecated but still available in the jar file as AmberFromPileupApplication. See below for more details.  

## R Dependencies
Segmentation is done with the Bioconductor [copynumber](http://bioconductor.org/packages/release/bioc/html/copynumber.html) package.

This can be installed in R with the following commands:
```
   source("https://bioconductor.org/biocLite.R")
   biocLite("copynumber")
```

## Mandatory Arguments

Argument | Description 
---|---
tumor | Name of the tumor sample
tumor_bam | Path to (indexed) tumor bam file
reference | Name of the reference sample
reference_bam | Path to (indexed) reference bam file
output_dir | Path to the output directory
ref_genome | Path to the ref genome fasta file
bed | Path to bed file containing likely heterozygous sites (see below). Gz files supported.  

The bed file used by HMF (GermlineHetPon.hg19.bed.gz) is available to download from [HMF-Pipeline-Resources.](https://resources.hartwigmedicalfoundation.nl) The sites were chosen by running the GATK HaplotypeCaller over 1700 germline samples and then selecting all SNP sites which are heterozygous in 800 to 900 of the samples. The 1.3 million sites provided in this file typically result in 450k+ BAF points. A HG38 equivalent is also available.

## Optional Arguments

Argument | Default | Description 
---|---|---
threads | 1 | Number of threads to use
min_mapping_quality | 1| Minimum mapping quality for an alignment to be used
min_base_quality | 13| Minimum quality for a base to be considered
min_depth_percent | 0.5 | Only include reference sites with read depth within min percentage of median reference read depth
max_depth_percent | 1.5 | Only include reference sites with read depth within max percentage of median reference read depth
min_het_af_percent | 0.4 | Minimum allelic frequency to be considered heterozygous
max_het_af_percent | 0.65 | Maximum allelic frequency to be considered heterozygous

## Example Usage

```
java -Xmx48G -cp amber.jar com.hartwig.hmftools.amber.AmberApplication \
   -threads 12 \
   -tumor TUMOR \
   -tumor_bam /path/to/tumor/bam/TUMOR.bam \
   -reference REFERENCE \
   -reference_bam /path/to/reference/bam/REFERENCE.bam \
   -output_dir /run_dir/amber/ \
   -ref_genome /path/to/refGenome/refGenome.fasta \
   -bed /path/to/GermlineHetPon.hg19.bed.gz 
```

## Performance Characteristics
Performance numbers were taken from a 72 core machine using COLO829 data with an average read depth of 35 and 93 in the normal and tumor respectively. Elapsed time is measured in minutes. CPU time is minutes spent in user mode. 

Reading directly from the bam, AMBER has the following characteristics:

Amber Method | Threads | Elapsed Time| CPU Time 
---|---|---|---
Bam | 1 | 77 | 83 
Bam | 2 | 43 | 91
Bam | 4 | 22 | 113
Bam | 8 | 14 | 110
Bam | 16 | 10 | 115 
Bam | 32 | 6 | 118
Bam | 64 | 4 | 168

For comparison, the deprecated pileup method has the following characteristics:

Amber Method | Threads | Elapsed Time| CPU Time 
---|---|---|---
Pileup | 1 | 180 | 180 
Pileup | 64 | 44 | 272 

## Output
File | Description
--- | ---
TUMOR.amber.baf | Tab separated values (TSV) containing reference and tumor BAF at each heterozygous site.
TUMOR.amber.baf.pcf | TSV of BAF segments using PCF algorithm.
TUMOR.amber.qc | Contains median tumor baf and QC status. FAIL may indicate contamination in sample. 
TUMOR.amber.vcf.gz | Similar information as BAF file but in VCF format. This file is not used by PURPLE.
 
## Comparison to pileup method
Calculating the BAF directly from the bams is functionally equivalent to the pileup method when using the following samtools arguments:

``` -A -B -x -Q 13 -q 1 -f /path/to/refGenome/refGenome.fasta ```

# AMBER From Pileup - Deprecated

This version of AMBER relies on mpileups of the reference and tumor samples sliced at likely heterozygous locations. Sambamba (or samtools) can be used to generate the mpileups. 

Example generation:

```
export PATH=/path/to/samtools/:$PATH

sambamba mpileup -t 6 /
    -L /path/to/bed/HeterozygousLocations.bed /
    /path/to/bam/REFERENCE.bam /
    --samtools -A -B -x -Q 13 -q 1 -f /path/to/refGenome/refGenome.fasta /
    > /path/to/mpileup/REFERENCE.mpileup

sambamba mpileup -t 6 /
    -L /path/to/bed/HeterozygousLocations.bed /
    /path/to/bam/TUMOR.bam /
    --samtools -A -B -x -Q 13 -q 1 -f /path/to/refGenome/refGenome.fasta /
    > /path/to/mpileup/TUMOR.mpileup

```

## Usage

Argument | Default | Description
---|---|---
-sample | None | Name of tumor sample
-reference | None | Location of reference mpileup file
-tumor | None | Location of tumor mpileup file
-output_dir | None | Directory to write output
-min_depth_percent | 0.5 | Only include reference positions with read depth within min percentage of median
-min_depth_percent | 1.5 | Only include reference positions with read depth within max percentage of median
-min_het_af_percent | 0.4 | Minimum allelic frequency to be considered heterozygous
-max_het_af_percent | 0.65 | Maximum allelic frequency to be considered heterozygous

Arguments without default values are mandatory.

## Example Usage

Please note the application name has changed from earlier versions.

```
java -cp amber.jar com.hartwig.hmftools.amber.pileup.AmberFromPileupApplication \ 
    -sample TUMOR \
    -output_dir /run_dir/amber \
    -reference /path/to/mpileup/REFERENCE.mpileup \
    -tumor /path/to/mpileup/TUMOR.mpileup
```
