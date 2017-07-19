const { mixpanel, ga } = global;

const track = (eventCategory, { action, label, ...otherProperties }) => {
  if (mixpanel) {
    mixpanel.track(eventCategory, { action, label, ...otherProperties });
  }
  if (ga) {
    ga('send', {
        hitType: 'event',
        eventCategory: eventCategory,
        eventAction: action,
        eventLabel: label,
    });
  }
};

export default track;