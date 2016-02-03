/*
 * Copyright 2016(c) The Ontario Institute for Cancer Research. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the GNU Public
 * License v3.0. You should have received a copy of the GNU General Public License along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

(function() {
  'use strict';

  var module = angular.module('icgc.common.codetable', []);

  /**
   * Binds misc. code/value lookup
   */
  module.service('CodeTable', function() {

    var translateLookup = {
      // Mutation type
      'single base substitution': 'Substitution',
      'insertion of <=200bp': 'Insertion',
      'deletion of <=200bp': 'Deletion',
      'multiple base substitution (>=2bp and <=200bp)': 'MSub',

      // OOZIE worflow status
      'NOT_FOUND': 'Not Found',
      'RUNNING': 'Running',
      'SUCCEEDED': 'Succeeded',
      'FAILED': 'Failed',
      'KILLED': 'Cancelled',
      'PREP': 'In Preparation',
      'FINISHING': 'Cleaning Up', // This is an artificial state

      // Functional Impact prediction categories
      'TOLERATED': 'Tolerated',
      'DAMAGING': 'Damaging',

      // GO Ontology
      'colocalizes_with': 'Colocalizes With',
      'contributes_to': 'Contributes To',

      // Biotype
      'lincRNA': 'lincRNA',
      'miRNA': 'miRNA',
      'snRNA': 'snRNA',
      'snoRNA': 'snoRNA',
      'rRNA': 'rRNA',
      '3prime_overlapping_ncrna': '3\' Overlapping ncRNA',
      'Mt_rRNA': 'Mt rRNA',

      // Facet Titles
      'id': 'Project',
      'projectId': 'Project',
      'primarySite': 'Primary Site',
      'primaryCountries': 'Country',
      'tumourStageAtDiagnosis': 'Tumour Stage',
      'vitalStatus': 'Vital Status',
      'diseaseStatusLastFollowup': 'Disease Status',
      'relapseType': 'Relapse Type',
      'ageAtDiagnosisGroup': 'Age at Diagnosis',
      'availableDataTypes': 'Available Data Type',
      'analysisTypes': 'Donor Analysis Type',
      'list': 'Gene sets',
      'verificationStatus': 'Verification Status',
      'consequenceType': 'Consequence Type',
      'functionalImpact': 'Functional Impact',
      'sequencingStrategy': 'Analysis Type',
      'tumourType': 'Tumour Type',
      'specimenType': 'Specimen Type',

      'studies': 'Study',
      'repoName': 'Repository',
      'fileName': 'File',
      'study': 'Only Files in Study',
      'fileFormat': 'File Format',
      'dataType': 'Data Type',
      'donorStudy': 'Only Donors in Study',
      'projectCode': 'Project',

      // Donor states
      'state': 'Donor Molecular Data',
      'live': 'Present in DCC',
      'pending': 'Absent from DCC'
    };

    var tooltipLookup = {
      // Sequencing analysis types (Sequencing strategy)
      'WGS': 'Whole Genome Sequencing - random sequencing of the whole genome.',
      'WGA': 'Whole Genome Amplification followed by random sequencing.',
      'WXS': 'Random sequencing of exonic regions selected from the genome.',
      'DNA-Seq': 'DNA sequencing using next-generation sequencing (NGS)',
      'RNA-Seq': 'Random sequencing of whole transcriptome, also known as Whole Transcriptome Shotgun Sequencing, ' +
                 'or WTSS',
      'miRNA-Seq': 'Micro RNA sequencing strategy designed to capture post-transcriptional RNA elements and ' +
                   'include non-coding functional elements.',
      'ncRNA-Seq': 'Capture of other non-coding RNA types, including post-translation modification types such as ' +
                   'snRNA (small nuclear RNA) or snoRNA (small nucleolar RNA), or expression regulation types such ' +
                   'as siRNA (small interfering RNA) or piRNA/piwi/RNA (piwi-interacting RNA).',
      'WCS': 'Random sequencing of a whole chromosome or other replicon isolated from a genome.',
      'CLONE': 'Genomic clone based (hierarchical) sequencing.',
      'POOLCLONE': 'Shotgun of pooled clones (usually BACs and Fosmids).',
      'AMPLICON': 'Sequencing of overlapping or distinct PCR or RT-PCR products. For example, metagenomic ' +
                  'community profiling using SSU rRNA.',
      'CLONEEND': 'Clone end (5\', 3\', or both) sequencing.',
      'FINISHING': 'Sequencing intended to finish (close) gaps in existing coverage.',
      'ChIP-Seq': 'chromatin immunoprecipitation.',
      'MNase-Seq': 'following MNase digestion.',
      'DNase-Hypersensitivity': 'Sequencing of hypersensitive sites, or segments of open chromatin that are more ' +
                                'readily cleaved by DNaseI.',
      'Bisulfite-Seq': 'MethylC-seq. Sequencing following treatment of DNA with bisulfite to convert cytosine ' +
                       'residues to uracil depending on methylation status.',
      'EST': 'Single pass sequencing of cDNA templates',
      'FL-cDNA': 'Full-length sequencing of cDNA templates',
      'CTS': 'Concatenated Tag Sequencing',
      'MRE-Seq': 'Methylation-Sensitive Restriction Enzyme Sequencing.',
      'MeDIP-Seq': 'Methylated DNA Immunoprecipitation Sequencing.',
      'MBD-Seq': 'Methyl CpG Binding Domain Sequencing.',
      'Tn-Seq': 'Quantitatively determine fitness of bacterial genes based on how many times a purposely seeded ' +
                'transposon gets inserted into each gene of a colony after some time.',
      'VALIDATION': 'CGHub special request: Independent experiment to re-evaluate putative variants.',
      'FAIRE-seq': 'Formaldehyde Assisted Isolation of Regulatory Elements',
      'SELEX': 'Systematic Evolution of Ligands by EXponential enrichment',
      'RIP-Seq': 'Direct sequencing of RNA immunoprecipitates (includes CLIP-Seq, HITS-CLIP and PAR-CLIP).',
      'ChIA-PET': 'Direct sequencing of proximity-ligated chromatin immunoprecipitates.',
      'OTHER': 'Library strategy not listed.',

      // Donor states
      'live': 'Present in DCC',
      'pending': 'Absent from DCC'
    };

    this.translate = function(id) {
      return translateLookup[id];
    };

    this.tooltip = function(id) {
      return tooltipLookup[id];
    };

    var countryCodeToName = {
      'af' : 'Afghanistan',
      'ax' : 'Aland Islands',
      'al' : 'Albania',
      'dz' : 'Algeria',
      'as' : 'American Samoa',
      'ad' : 'Andorra',
      'ao' : 'Angola',
      'ai' : 'Anguilla',
      'aq' : 'Antarctica',
      'ag' : 'Antigua And Barbuda',
      'ar' : 'Argentina',
      'am' : 'Armenia',
      'aw' : 'Aruba',
      'au' : 'Australia',
      'at' : 'Austria',
      'az' : 'Azerbaijan',
      'bs' : 'Bahamas',
      'bh' : 'Bahrain',
      'bd' : 'Bangladesh',
      'bb' : 'Barbados',
      'by' : 'Belarus',
      'be' : 'Belgium',
      'bz' : 'Belize',
      'bj' : 'Benin',
      'bm' : 'Bermuda',
      'bt' : 'Bhutan',
      'bo' : 'Bolivia',
      'ba' : 'Bosnia And Herzegovina',
      'bw' : 'Botswana',
      'bv' : 'Bouvet Island',
      'br' : 'Brazil',
      'io' : 'British Indian Ocean Territory',
      'bn' : 'Brunei Darussalam',
      'bg' : 'Bulgaria',
      'bf' : 'Burkina Faso',
      'bi' : 'Burundi',
      'kh' : 'Cambodia',
      'cm' : 'Cameroon',
      'ca' : 'Canada',
      'cv' : 'Cape Verde',
      'ky' : 'Cayman Islands',
      'cf' : 'Central African Republic',
      'td' : 'Chad',
      'cl' : 'Chile',
      'cn' : 'China',
      'cx' : 'Christmas Island',
      'cc' : 'Cocos (Keeling) Islands',
      'co' : 'Colombia',
      'km' : 'Comoros',
      'cg' : 'Congo',
      'cd' : 'Congo, Democratic Republic',
      'ck' : 'Cook Islands',
      'cr' : 'Costa Rica',
      'ci' : 'Cote D\'Ivoire',
      'hr' : 'Croatia',
      'cu' : 'Cuba',
      'cy' : 'Cyprus',
      'cz' : 'Czech Republic',
      'dk' : 'Denmark',
      'dj' : 'Djibouti',
      'dm' : 'Dominica',
      'do' : 'Dominican Republic',
      'ec' : 'Ecuador',
      'eg' : 'Egypt',
      'sv' : 'El Salvador',
      'gq' : 'Equatorial Guinea',
      'er' : 'Eritrea',
      'ee' : 'Estonia',
      'et' : 'Ethiopia',
      'eu' : 'European Union',
      'fk' : 'Falkland Islands (Malvinas)',
      'fo' : 'Faroe Islands',
      'fj' : 'Fiji',
      'fi' : 'Finland',
      'fr' : 'France',
      'gf' : 'French Guiana',
      'pf' : 'French Polynesia',
      'tf' : 'French Southern Territories',
      'ga' : 'Gabon',
      'gm' : 'Gambia',
      'ge' : 'Georgia',
      'de' : 'Germany',
      'gh' : 'Ghana',
      'gi' : 'Gibraltar',
      'gr' : 'Greece',
      'gl' : 'Greenland',
      'gd' : 'Grenada',
      'gp' : 'Guadeloupe',
      'gu' : 'Guam',
      'gt' : 'Guatemala',
      'gg' : 'Guernsey',
      'gn' : 'Guinea',
      'gw' : 'Guinea-Bissau',
      'gy' : 'Guyana',
      'ht' : 'Haiti',
      'hm' : 'Heard Island & Mcdonald Islands',
      'va' : 'Holy See (Vatican City State)',
      'hn' : 'Honduras',
      'hk' : 'Hong Kong',
      'hu' : 'Hungary',
      'is' : 'Iceland',
      'in' : 'India',
      'id' : 'Indonesia',
      'ir' : 'Iran, Islamic Republic Of',
      'iq' : 'Iraq',
      'ie' : 'Ireland',
      'im' : 'Isle Of Man',
      'il' : 'Israel',
      'it' : 'Italy',
      'jm' : 'Jamaica',
      'jp' : 'Japan',
      'je' : 'Jersey',
      'jo' : 'Jordan',
      'kz' : 'Kazakhstan',
      'ke' : 'Kenya',
      'ki' : 'Kiribati',
      'kr' : 'South Korea',
      'kw' : 'Kuwait',
      'kg' : 'Kyrgyzstan',
      'la' : 'Lao People\'s Democratic Republic',
      'lv' : 'Latvia',
      'lb' : 'Lebanon',
      'ls' : 'Lesotho',
      'lr' : 'Liberia',
      'ly' : 'Libyan Arab Jamahiriya',
      'li' : 'Liechtenstein',
      'lt' : 'Lithuania',
      'lu' : 'Luxembourg',
      'mo' : 'Macao',
      'mk' : 'Macedonia',
      'mg' : 'Madagascar',
      'mw' : 'Malawi',
      'my' : 'Malaysia',
      'mv' : 'Maldives',
      'ml' : 'Mali',
      'mt' : 'Malta',
      'mh' : 'Marshall Islands',
      'mq' : 'Martinique',
      'mr' : 'Mauritania',
      'mu' : 'Mauritius',
      'yt' : 'Mayotte',
      'mx' : 'Mexico',
      'fm' : 'Micronesia, Federated States Of',
      'md' : 'Moldova',
      'mc' : 'Monaco',
      'mn' : 'Mongolia',
      'me' : 'Montenegro',
      'ms' : 'Montserrat',
      'ma' : 'Morocco',
      'mz' : 'Mozambique',
      'mm' : 'Myanmar',
      'na' : 'Namibia',
      'nr' : 'Nauru',
      'np' : 'Nepal',
      'nl' : 'Netherlands',
      'an' : 'Netherlands Antilles',
      'nc' : 'New Caledonia',
      'nz' : 'New Zealand',
      'ni' : 'Nicaragua',
      'ne' : 'Niger',
      'ng' : 'Nigeria',
      'nu' : 'Niue',
      'nf' : 'Norfolk Island',
      'mp' : 'Northern Mariana Islands',
      'no' : 'Norway',
      'om' : 'Oman',
      'pk' : 'Pakistan',
      'pw' : 'Palau',
      'ps' : 'Palestinian Territory, Occupied',
      'pa' : 'Panama',
      'pg' : 'Papua New Guinea',
      'py' : 'Paraguay',
      'pe' : 'Peru',
      'ph' : 'Philippines',
      'pn' : 'Pitcairn',
      'pl' : 'Poland',
      'pt' : 'Portugal',
      'pr' : 'Puerto Rico',
      'qa' : 'Qatar',
      're' : 'Reunion',
      'ro' : 'Romania',
      'ru' : 'Russian Federation',
      'rw' : 'Rwanda',
      'bl' : 'Saint Barthelemy',
      'sh' : 'Saint Helena',
      'kn' : 'Saint Kitts And Nevis',
      'lc' : 'Saint Lucia',
      'mf' : 'Saint Martin',
      'pm' : 'Saint Pierre And Miquelon',
      'vc' : 'Saint Vincent And Grenadines',
      'ws' : 'Samoa',
      'sm' : 'San Marino',
      'st' : 'Sao Tome And Principe',
      'sa' : 'Saudi Arabia',
      'sn' : 'Senegal',
      'rs' : 'Serbia',
      'sc' : 'Seychelles',
      'sl' : 'Sierra Leone',
      'sg' : 'Singapore',
      'sk' : 'Slovakia',
      'si' : 'Slovenia',
      'sb' : 'Solomon Islands',
      'so' : 'Somalia',
      'za' : 'South Africa',
      'gs' : 'South Georgia And Sandwich Isl.',
      'es' : 'Spain',
      'lk' : 'Sri Lanka',
      'sd' : 'Sudan',
      'sr' : 'Suriname',
      'sj' : 'Svalbard And Jan Mayen',
      'sz' : 'Swaziland',
      'se' : 'Sweden',
      'ch' : 'Switzerland',
      'sy' : 'Syrian Arab Republic',
      'tw' : 'Taiwan',
      'tj' : 'Tajikistan',
      'tz' : 'Tanzania',
      'th' : 'Thailand',
      'tl' : 'Timor-Leste',
      'tg' : 'Togo',
      'tk' : 'Tokelau',
      'to' : 'Tonga',
      'tt' : 'Trinidad And Tobago',
      'tn' : 'Tunisia',
      'tr' : 'Turkey',
      'tm' : 'Turkmenistan',
      'tc' : 'Turks And Caicos Islands',
      'tv' : 'Tuvalu',
      'ug' : 'Uganda',
      'ua' : 'Ukraine',
      'ae' : 'United Arab Emirates',
      'gb' : 'United Kingdom',
      'us' : 'United States',
      'um' : 'United States Outlying Islands',
      'uy' : 'Uruguay',
      'uz' : 'Uzbekistan',
      'vu' : 'Vanuatu',
      've' : 'Venezuela',
      'vn' : 'Viet Nam',
      'vg' : 'Virgin Islands, British',
      'vi' : 'Virgin Islands, U.S.',
      'wf' : 'Wallis And Futuna',
      'eh' : 'Western Sahara',
      'ye' : 'Yemen',
      'zm' : 'Zambia',
      'zw' : 'Zimbabwe'
    };
    var countryNameToCode = _.invert (countryCodeToName);

    this.countryCode = function (countryName) {
      return _.get (countryNameToCode, countryName, '');
    };
    // Translates non-standard codes to ISO codes.
    this.translateCountryCode = function (countryCode) {
      if ('uk' === countryCode) {
        return 'gb';
      }

      return countryCode;
    };
    this.countryName = function (countryCode) {
      return _.get (countryCodeToName, countryCode, '');
    };

  });

})();
