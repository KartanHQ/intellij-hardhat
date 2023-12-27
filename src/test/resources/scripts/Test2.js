const {ethers} = require("hardhat");

async function deploy() {
  const [owner, alice, bob] = await ethers.getSigners();
  const stio = await (await ethers.getContractFactory("Stio")).deploy();

  //Code1

  return { stio: stio, owner, alice, bob };
}