export const ensureArray = (array) => _.isArray(array) ? array : [];

export const ensureString = (string) => _.isString(string) ? string.trim() : '';

export const partiallyContainsIgnoringCase = (phrase, keyword) => {
    if (_.isEmpty(phrase)) {
      return false;
    }

    const capitalizedPhrase = phrase.toUpperCase();
    const capitalizedKeyword = keyword.toUpperCase();

    const tokens = [capitalizedKeyword].concat (words(capitalizedKeyword));
    const matchKeyword = _(tokens)
      .unique()
      .find (function (token) {
        return _.contains(capitalizedPhrase, token);
      });

    return !_.isUndefined(matchKeyword);
  };

const words = (phrase) => _.words(phrase, /[^, ]+/g);