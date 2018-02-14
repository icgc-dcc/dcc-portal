module.exports = {
  babelrc: false,
  presets: ['babel-preset-react', 'babel-preset-es2015', 'babel-preset-es2016'].map(
    require.resolve
  ),
  plugins: [
    'babel-plugin-syntax-dynamic-import',
    'babel-plugin-dynamic-import-webpack',
    'babel-plugin-transform-decorators-legacy',
    'babel-plugin-transform-decorators',
    'babel-plugin-transform-class-properties',
    'babel-plugin-syntax-trailing-function-commas',
    'babel-plugin-transform-object-rest-spread',
    'babel-plugin-add-module-exports',
    'babel-plugin-transform-async-to-generator',
    'babel-plugin-transform-react-remove-prop-types',
    'babel-plugin-transform-react-constant-elements',
    'babel-plugin-transform-react-inline-elements',
  ]
    .map(require.resolve)
    .concat([
      [
        require.resolve('babel-plugin-transform-runtime'),
        {
          helpers: false,
          polyfill: false,
          regenerator: true,
        },
      ],
      ['babel-root-import', { rootPathSuffix: 'app' }],
    ]),
};
