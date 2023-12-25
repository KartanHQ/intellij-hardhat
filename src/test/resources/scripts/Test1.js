const {ethers} = require("hardhat");

async function deploy() {
  const [owner, alice, bob] = await ethers.getSigners();
  const Stio = await ethers.getContractFactory("Stio");
  const stio = await Stio.deploy();



  return { stio: stio, owner, alice, bob };
}