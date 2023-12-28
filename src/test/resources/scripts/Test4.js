const {ethers} = require("hardhat");

async function myCustomDeploy() {
  const [contractOwner, alice, bob] = await ethers.getSigners();
  const Stio = await ethers.getContractFactory();
  const stio = await Stio.deploy();

  //Code1

  return { stio: stio, contractOwner, alice, bob };
}