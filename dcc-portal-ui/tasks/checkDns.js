const dns = require('dns');
const chalk = require('chalk');

const isWindows = /^win/.test(process.platform);

dns.lookup('local.dcc.icgc.org', (err, addresses, family) => {
  if (addresses === undefined) {
    console.log(chalk.bold.red(`DNS failed for ${chalk.underline('local.dcc.icgc.org')}`));
    console.log(chalk.red(`Please add the following to your host file:`));
    console.log(chalk.white(`127.0.0.1 local.dcc.icgc.org`));

    if (isWindows) {
      console.log(chalk.red(`You can do that manually, or run ${chalk.white('\`npm run sethost\`')} in a command prompt that has been ${chalk.bold('run as an administrator')}`));
    } else {
      console.log(chalk.red(`You can do that manually, or run ${chalk.white('\`sudo npm run sethost\`')}`));
    }

    process.exitCode = 1;
  }
});