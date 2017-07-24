const { mixpanel, ga } = global;
const _ = require('lodash');

const track = (eventCategory, { action, label, value, ...otherProperties }) => {
  if (process.env.NODE_ENV === 'development') {
    console.log('track', eventCategory, _.omitBy({ action, label, value, ...otherProperties }, _.isNil));
    return;
  }
  if (mixpanel) {
    mixpanel.track(eventCategory, _.omitBy({ action, label, value, ...otherProperties }, _.isNil));
  }
  if (ga) {
    ga('send', _.omitBy({
        hitType: 'event',
        eventCategory: eventCategory,
        eventAction: action,
        eventLabel: label,
    }, _.isNil));
  }
};

export default track;