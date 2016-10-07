// Test suite for PqlTranslateService

describe('Testing PqlTranslationService', function() {
  var PqlTranslationService;

  beforeEach(module('icgc'));

  beforeEach(inject(function (_PqlTranslationService_) {
    window._gaq = [];
    PqlTranslationService = _PqlTranslationService_;
  }));

  it('Testing fromPql()', function() {
     var sourcePql = "eq(test, 123)";
     var expectedJson = [{op: "eq", field: "test", values: [123]}];

     var tree = PqlTranslationService.fromPql(sourcePql);
     expect(tree).toEqual(expectedJson);
  });

  it('Testing toPql()', function() {
    var sourceJson = [{op: "eq", field: "test", values: [123]}];
    var expectedPql = "eq(test,123)";

    var pql = PqlTranslationService.toPql (sourceJson);
    expect (pql).toEqual(expectedPql);
  });

  it('Testing fromPql()', function() {
     var sourcePql = "eq(test, '123')";
     var expectedJson = [{op: "eq", field: "test", values: ['123']}];

     var tree = PqlTranslationService.fromPql(sourcePql);
     expect(tree).toEqual(expectedJson);
  });

  it('Testing fromPql() for pql: eq(donor.gender,"male")', function() {
     var sourcePql = 'eq(donor.gender,"male")';
     var expectedJson = [{op: "eq", field: "donor.gender", values: ['male']}];

     var tree = PqlTranslationService.fromPql(sourcePql);
     expect(tree).toEqual(expectedJson);
  });

  it('Testing fromPql() with an invalid pql: "eq(test,)"', function() {
     var pql = "eq(test,)";

     expect(function () {
      PqlTranslationService.fromPql (pql);
    }).toThrow();
  });

  it('Testing fromPql() with an invalid pql: "count(),select(*),eq(test,123)"', function() {
     var pql = "count(),select(*),eq(test,123)";

     expect(function () {
      PqlTranslationService.fromPql (pql);
    }).toThrow();
  });

  it('Testing fromPql() with an invalid pql: "count(),eq(test,123),sort(+donor.id)"', function() {
     var pql = "count(),eq(test,123),sort(+donor.id)";

     expect(function () {
      PqlTranslationService.fromPql (pql);
    }).toThrow();
  });

  it('Testing toPql()', function() {
    var sourceJson = [{op: "eq", field: "test", values: ['123']}];
    var expectedPql = 'eq(test,"123")';

    var pql = PqlTranslationService.toPql (sourceJson);
    expect (pql).toEqual(expectedPql);
  });

});

